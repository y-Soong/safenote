package com.prafta.app.req.req07.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.app.req.req07.application.command.AttdReqInsertCommand;
import com.prafta.app.req.req07.application.param.AttdCorrectionParam;
import com.prafta.app.req.req07.application.param.OvertimeParam;
import com.prafta.app.req.req07.application.param.SchedModifyParam;
import com.prafta.app.req.req07.dto.request.SlotRequest;
import com.prafta.app.req.req07.dto.response.RegisterReqResponse;
import com.prafta.app.req.req07.dto.response.SchedOptionResponse;
import com.prafta.app.req.req07.dto.response.result.ActualAttdWindowResult;
import com.prafta.app.req.req07.dto.response.result.ScheduleWindowResult;
import com.prafta.app.req.req07.dto.response.result.SchedOptionResult;
import com.prafta.app.req.req07.mapper.AppReq07Mapper;
import com.prafta.app.req.req07.service.AppReq07Service;
import com.prafta.app.req.req09.service.AttdApprovalLineService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.attd07.util.AttdReqTypeUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-007: 모바일 앱 근태 요청 등록 서비스 구현.
 *
 * <p>설계 원칙 (plan §6.4):
 * <ul>
 *   <li>식별값은 Param 의 cmpnyCd/siteCd/userCd 만 사용 (IDOR — Param.from 단계에서 JWT 강제).</li>
 *   <li>각 endpoint 별로 단일 @Transactional 안에서 REQ_ID 채번 + slots × INSERT.</li>
 *   <li>중복 요청 차단 (P10) 은 트랜잭션 시작 직후 한 번 검사.</li>
 *   <li>마감/스케줄 존재 등 비즈니스 가드는 plan §4.3 P11 명시: 본 1차 구현에서는
 *       1) 마감 가드 — 본 패키지 단독 의존 추가 회피 위해 별도 추가하지 않고 follow-up.
 *          (사용자 노출은 ATTD_400_099 로 향후 연결 — AttdCloseService 등.)
 *       2) 본인 스케줄 존재 가드 — 동일하게 follow-up.
 *       3) 본 트랜잭션의 INSERT 자체는 fail-closed 유지 (구조적 검증 + 중복 차단 + nodeCd allow).
 *   </li>
 *   <li>결재선 INSERT 는 prafta-app-009 분리 — 메서드 끝에 마커 주석.</li>
 *   <li>알림 발송 (push outbox INSERT) 은 P4 follow-up — 본 작업 미포함.</li>
 * </ul>
 *
 * <p>입력 검증 헬퍼는 본 클래스 내부의 private 정적 메서드로 둔다 (다른 모듈에서 재사용 안 함).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppReq07ServiceImpl implements AppReq07Service {

    private final AppReq07Mapper mapper;
    /** prafta-app-009: 결재 분기/라인 INSERT 공용 서비스(같은 @Transactional 참여). */
    private final AttdApprovalLineService attdApprovalLineService;
    /** prafta-app-009 F12: 근태/스케줄 마감 가드(web 빈 재사용 — 로직 미복제). */
    private final AttdCloseService attdCloseService;

    /** REQ_STATUS = '01' 신청 (등록 직후 고정 — P3). */
    private static final String REQ_STATUS_REQUESTED = AttdReqTypeUtils.REQ_STATUS_REQUESTED;

    /** prafta-app-009 F15: advisory lock 타임아웃(초). 짧게 — 동시 중복 제출 직렬화용. */
    private static final int DUP_LOCK_TIMEOUT_SEC = 3;

    // ============================================================
    // 1) 스케줄 수정 (POST /appApi/req07/sched-modify)
    // ============================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterReqResponse registerSchedModify(SchedModifyParam param) {

        // ----- 구조 검증 -----
        validateSlotsSize(param.slots());
        validateNoDuplicateWorkSeq(param.slots());
        if (!StringUtils.hasText(param.reqReason())) {
            throw new ApiException(AttdErrorCode.ATTD_400_096);
        }
        for (SlotRequest s : param.slots()) {
            if (!StringUtils.hasText(s.getSchCd())) {
                throw new ApiException(AttdErrorCode.ATTD_400_097);
            }
        }

        // ----- prafta-app-009 가드(INSERT 시작 전 fail-closed) -----
        //   F12 마감: 해당 월이 사용자 부서 마감 커버리지에 포함되면 거부(ATTD_400_099).
        assertNotClosed(param.cmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());
        //   F13 스케줄 존재: 배정된 근무일(근무계획 행)이 아니면 거부(ATTD_400_098).
        assertWorkPlanExists(param.cmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());

        // ----- prafta-app-009 F15: 중복 차단 SELECT→INSERT race window 직렬화(advisory lock) -----
        //   PRAFTA-APP-022 TOCTOU: 룰A 상호배제(OT↔스케줄수정 cross-type)는 타입별 dupLock 만으론 직렬화되지
        //   않으므로, 요청유형 제외 공통 mutex 를 [공통키 → 타입별키] 순서로 추가 획득한다(registerOvertime 과 동일 순서 → 데드락 회피).
        String mutexKey = ruleAMutexKey(param.cmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());
        String lockKey = dupLockKey(param.cmpnyCd(), param.siteCd(), param.userCd(),
                param.workYmd(), AttdReqTypeUtils.REQ_TYPE_SCHED_MODIFY);
        String reqId = null; // 응답/로그용 대표값(첫 슬롯의 REQ_ID)
        List<Integer> workSeqs = new ArrayList<>(param.slots().size());
        acquireDupLock(mutexKey);   // (1) 공통 mutex 먼저
        try {
        acquireDupLock(lockKey);    // (2) 타입별 dup 락 다음
        try {
            // ----- 중복 요청 차단 (P10) -----
            int dup = mapper.countDuplicateReq(
                    param.cmpnyCd(), param.siteCd(), param.userCd(),
                    param.workYmd(), AttdReqTypeUtils.REQ_TYPE_SCHED_MODIFY);
            if (dup > 0) {
                throw new ApiException(AttdErrorCode.ATTD_400_090);
            }

            // ----- PRAFTA-APP-022 룰A2/A3: 활성 초과근무 요청(생성03·수정04, 대기01+승인02) 존재 시
            //   스케줄수정 거부(상호배제). 그날 전체(WORK_SEQ 무관). lock 후·INSERT 전 fail-closed. -----
            int activeOt = mapper.countActiveOvertimeReq(
                    param.cmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());
            if (activeOt > 0) {
                log.info("[prafta-app-022] 스케줄수정 거부: 활성 초과근무 요청 존재 — userCd={}, workYmd={}, activeOt={}",
                        param.userCd(), param.workYmd(), activeOt);
                throw new ApiException(AttdErrorCode.ATTD_400_107);
            }

            // ----- INSERT × slots.length (REQ_ID 는 PK 단일 컬럼이므로 slot 마다 새로 채번) -----
            for (SlotRequest s : param.slots()) {
                String slotReqId = mapper.selectNextReqId(param.cmpnyCd());
                if (reqId == null) reqId = slotReqId;
                AttdReqInsertCommand cmd = new AttdReqInsertCommand(
                        slotReqId,
                        param.cmpnyCd(), param.siteCd(), param.userCd(),
                        AttdReqTypeUtils.REQ_TYPE_SCHED_MODIFY,
                        null,                            // TARGET_ID (스케줄 수정은 null)
                        REQ_STATUS_REQUESTED,
                        param.reqReason(),
                        param.workYmd(), param.nodeCd(),
                        s.getWorkSeq(),
                        null, null, null, null,          // START/END_DATE/TIME (스케줄 수정은 null)
                        s.getSchCd(),                    // SCH_CD (REQ_TYPE='10' 전용)
                        param.userCd()                   // INSERT_NO
                );
                mapper.insertAttdReq(cmd);
                // prafta-app-009: 슬롯(REQ_ID)마다 결재 분기/라인 처리(같은 @Transactional).
                attdApprovalLineService.applyApprovalFlow(
                        param.cmpnyCd(), param.siteCd(), param.userCd(), slotReqId,
                        param.approverUserCds(), param.presetId(), param.userCd());
                workSeqs.add(s.getWorkSeq());
            }
        } finally {
            releaseDupLock(lockKey);    // (2) 타입별 락 해제(획득 역순)
        }
        } finally {
            releaseDupLock(mutexKey);   // (1) 공통 mutex 해제
        }

        log.info("[prafta-app-007] 스케줄 수정 요청 등록 — reqId={}, userCd={}, workYmd={}, slots={}",
                reqId, param.userCd(), param.workYmd(), workSeqs.size());

        return new RegisterReqResponse(
                reqId,
                AttdReqTypeUtils.REQ_TYPE_SCHED_MODIFY,
                REQ_STATUS_REQUESTED,
                workSeqs);
    }

    // ============================================================
    // 2) 근태 보정 (POST /appApi/req07/attd-correction)
    // ============================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterReqResponse registerAttdCorrection(AttdCorrectionParam param) {

        // ----- 구조 검증 -----
        validateSlotsSize(param.slots());
        validateNoDuplicateWorkSeq(param.slots());
        if (!StringUtils.hasText(param.reqReason())) {
            throw new ApiException(AttdErrorCode.ATTD_400_096);
        }
        validateSlotsTimes(param.slots());

        // ----- PRAFTA-APP-022 룰C: 미래(미도래) 일자 근태 보정 차단 -----
        //   정책 §11.2 "과거·현재만 신청 가능, 미래 차단". 당일은 허용(미래만 차단 — 확정 결정 4).
        //   순수 날짜 비교(lock 불필요)이므로 구조검증 직후 배치. 프론트 게이팅과 별개의 백엔드 권위 가드(변조/직접호출 방어).
        //   ※ 룰D 회귀: 출퇴근 실적 없는 과거/당일은 본 가드를 통과하여 REQ_TYPE='01' 생성 경로로 정상 진입한다(무변경).
        if (param.workYmd() != null && param.workYmd().compareTo(todayYmd()) > 0) {
            log.info("[prafta-app-022] 근태 보정 거부: 미래 일자 — userCd={}, workYmd={}",
                    param.userCd(), param.workYmd());
            throw new ApiException(AttdErrorCode.ATTD_400_109);
        }

        // ----- prafta-app-009 가드(INSERT 시작 전 fail-closed) -----
        //   F12 마감 / F13 본인 근무계획 존재. (근태 보정도 배정된 근무일 대상으로 한정.)
        assertNotClosed(param.cmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());
        assertWorkPlanExists(param.cmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());

        // ----- prafta-app-009 F15: 보정 제출 직렬화(advisory lock — 01/02 혼합 포함 그날 단위) -----
        String lockKey = dupLockKey(param.cmpnyCd(), param.siteCd(), param.userCd(),
                param.workYmd(), "ATTD_CORR");
        String reqId = null; // 응답/로그용 대표값(첫 슬롯의 REQ_ID)
        List<Integer> workSeqs = new ArrayList<>(param.slots().size());
        String responseReqType;
        acquireDupLock(lockKey);
        try {
            // ----- 자동 분기 (Q2) — slot 별 사전 조회 -----
            List<String> targetIds = new ArrayList<>(param.slots().size());
            List<String> reqTypes = new ArrayList<>(param.slots().size());
            boolean hasCreate = false;
            boolean hasModify = false;
            for (SlotRequest s : param.slots()) {
                String existingAttdId = mapper.selectExistingAttdId(
                        param.cmpnyCd(), param.siteCd(), param.userCd(),
                        param.workYmd(), s.getWorkSeq());
                if (existingAttdId != null) {
                    targetIds.add(existingAttdId);
                    reqTypes.add(AttdReqTypeUtils.REQ_TYPE_ATTD_MODIFY);
                    hasModify = true;
                } else {
                    targetIds.add(null);
                    reqTypes.add(AttdReqTypeUtils.REQ_TYPE_ATTD_CREATE);
                    hasCreate = true;
                }
            }

            // ----- 중복 요청 차단 (P10) — 각 사용한 REQ_TYPE 에 대해 검사 -----
            // 같은 일자 동일 REQ_TYPE 의 미처리 행 존재 시 차단. 한 요청에서 01/02 가 섞이면 둘 다 검사.
            if (hasCreate) {
                int dup = mapper.countDuplicateReq(
                        param.cmpnyCd(), param.siteCd(), param.userCd(),
                        param.workYmd(), AttdReqTypeUtils.REQ_TYPE_ATTD_CREATE);
                if (dup > 0) throw new ApiException(AttdErrorCode.ATTD_400_090);
            }
            if (hasModify) {
                int dup = mapper.countDuplicateReq(
                        param.cmpnyCd(), param.siteCd(), param.userCd(),
                        param.workYmd(), AttdReqTypeUtils.REQ_TYPE_ATTD_MODIFY);
                if (dup > 0) throw new ApiException(AttdErrorCode.ATTD_400_090);
            }

            // ----- INSERT × slots.length (REQ_ID 는 PK 단일 컬럼이므로 slot 마다 새로 채번) -----
            for (int i = 0; i < param.slots().size(); i++) {
                SlotRequest s = param.slots().get(i);
                String slotReqId = mapper.selectNextReqId(param.cmpnyCd());
                if (reqId == null) reqId = slotReqId;
                AttdReqInsertCommand cmd = new AttdReqInsertCommand(
                        slotReqId,
                        param.cmpnyCd(), param.siteCd(), param.userCd(),
                        reqTypes.get(i),
                        targetIds.get(i),
                        REQ_STATUS_REQUESTED,
                        param.reqReason(),
                        param.workYmd(), param.nodeCd(),
                        s.getWorkSeq(),
                        s.getStartDate(), s.getStartTime(),
                        s.getEndDate(), s.getEndTime(),
                        null,                            // SCH_CD (보정 미사용)
                        param.userCd()
                );
                mapper.insertAttdReq(cmd);
                // prafta-app-009: 슬롯(REQ_ID)마다 결재 분기/라인 처리(같은 @Transactional).
                attdApprovalLineService.applyApprovalFlow(
                        param.cmpnyCd(), param.siteCd(), param.userCd(), slotReqId,
                        param.approverUserCds(), param.presetId(), param.userCd());
                workSeqs.add(s.getWorkSeq());
            }

            // 응답 reqType: 생성/수정 혼합이면 'MIXED'.
            if (hasCreate && hasModify) {
                responseReqType = "MIXED";
            } else if (hasModify) {
                responseReqType = AttdReqTypeUtils.REQ_TYPE_ATTD_MODIFY;
            } else {
                responseReqType = AttdReqTypeUtils.REQ_TYPE_ATTD_CREATE;
            }
        } finally {
            releaseDupLock(lockKey);
        }

        // (응답 reqType 산출은 락 블록 내에서 완료됨)
        log.info("[prafta-app-007] 근태 보정 요청 등록 — reqId={}, userCd={}, workYmd={}, slots={}, reqType={}",
                reqId, param.userCd(), param.workYmd(), workSeqs.size(), responseReqType);

        return new RegisterReqResponse(reqId, responseReqType, REQ_STATUS_REQUESTED, workSeqs);
    }

    // ============================================================
    // 3) 초과근무 (POST /appApi/req07/overtime)
    // ============================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterReqResponse registerOvertime(OvertimeParam param) {

        // ----- 구조 검증 -----
        validateSlotsSize(param.slots());
        validateNoDuplicateWorkSeq(param.slots());
        if (!StringUtils.hasText(param.reqReason())) {
            throw new ApiException(AttdErrorCode.ATTD_400_096);
        }
        validateSlotsTimes(param.slots());

        // prafta-043: OT_TYPE(초과근무 유형) 전면 파기.
        //   - tb_user_attd_req.OT_TYPE 컬럼 자체를 제거(마이그 prafta-043-2-contract-drop-ot-type.sql).
        //   - 초과근무는 유형(연장/야간/휴일) 구분 없이 단일 '초과근무'로만 관리한다.
        //   - 종전 OT_TYPE allow-list 강제 + ATTD_400_095 발화는 prafta-app-016에서 이미 제거됨.

        // ----- prafta-app-009 F12 마감 가드(INSERT 시작 전 fail-closed) -----
        //   F13(스케줄 존재)은 OT 에 추가하지 않는다(기존 실근태 범위 가드 ATTD_400_104 로 충족).
        assertNotClosed(param.cmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());

        // ----- prafta-app-009 F15: OT 제출 직렬화(advisory lock) -----
        //   PRAFTA-APP-022 TOCTOU: 룰A 상호배제(OT↔스케줄수정 cross-type)를 위해 요청유형 제외 공통 mutex 를
        //   [공통키 → 타입별키] 순서로 추가 획득한다(registerSchedModify 와 동일 순서 → 데드락 회피).
        String mutexKey = ruleAMutexKey(param.cmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());
        String lockKey = dupLockKey(param.cmpnyCd(), param.siteCd(), param.userCd(),
                param.workYmd(), AttdReqTypeUtils.REQ_TYPE_OT_REGISTER);
        String reqId = null; // 응답/로그용 대표값(첫 슬롯의 REQ_ID)
        List<Integer> workSeqs = new ArrayList<>(param.slots().size());
        acquireDupLock(mutexKey);   // (1) 공통 mutex 먼저
        try {
        acquireDupLock(lockKey);    // (2) 타입별 dup 락 다음
        try {
            // ----- 중복 요청 차단 (P10) -----
            int dup = mapper.countDuplicateReq(
                    param.cmpnyCd(), param.siteCd(), param.userCd(),
                    param.workYmd(), AttdReqTypeUtils.REQ_TYPE_OT_REGISTER);
            if (dup > 0) {
                throw new ApiException(AttdErrorCode.ATTD_400_090);
            }

            // ===== prafta-app-017 등록 가드 — 모든 거부 게이트를 INSERT 시작 전에 모아 fail-closed =====
            //   순서: (이슈②) 스케줄수정 미처리(전일 차단) → (이슈②) 슬롯별 근태보정 미처리(구간 차단)
            //         → (이슈①) 슬롯별 스케줄 겹침(구간 차단). 위반 시 즉시 throw(부분 INSERT 방지).

            // ----- PRAFTA-APP-022 룰A1(이슈② 확장): 활성 스케줄수정(대기01+승인02) 존재 시 그날 OT 거부 -----
            //   확정 결정 1: 승인분도 충돌 범위 → IN('01','02'). 거부 코드는 신규 106(승인 포함 정확 문구).
            int activeSched = mapper.countActiveSchedModify(
                    param.cmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());
            if (activeSched > 0) {
                log.info("[prafta-app-022] OT 거부: 활성 스케줄수정 요청 존재 — userCd={}, workYmd={}, activeSched={}",
                        param.userCd(), param.workYmd(), activeSched);
                throw new ApiException(AttdErrorCode.ATTD_400_106);
            }

            // ----- 이슈① 겹침 검증용 근무계획 스케줄 1건 조회(없으면 전 구간 면제) -----
            ScheduleWindowResult schedule = mapper.selectWorkPlanSchedule(
                    param.cmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());

            // ----- slot 단위 가드(이슈② 근태보정 미처리 → 이슈① 스케줄 겹침) -----
            for (SlotRequest s : param.slots()) {
                // (이슈②) 해당 구간 근태보정 미처리(생성01·수정02) 존재 → 그 구간 거부.
                int pendCorr = mapper.countPendingAttdCorrectionBySlot(
                        param.cmpnyCd(), param.siteCd(), param.userCd(),
                        param.workYmd(), s.getWorkSeq());
                if (pendCorr > 0) {
                    log.info("[prafta-app-017] OT 미처리 근태보정 거부 — userCd={}, workYmd={}, workSeq={}, pendCorr={}",
                            param.userCd(), param.workYmd(), s.getWorkSeq(), pendCorr);
                    throw new ApiException(AttdErrorCode.ATTD_400_101);
                }
                // (prafta-app-019) OT 슬롯이 해당 구간 실제 근태기록 [CHECK_IN~CHECK_OUT] 범위 안에 포함되는지 검증.
                //   실근태 포함검증(신규 104) → 스케줄 겹침(기존 100) 순서. 둘 다 INSERT 시작 전 fail-closed.
                assertWithinActualAttdWindow(param.cmpnyCd(), param.siteCd(), param.userCd(),
                        param.workYmd(), s);
                // (이슈①) OT 시각이 해당 구간 정규 스케줄과 겹치면 거부.
                assertNoScheduleOverlap(param.workYmd(), schedule, s, param.userCd());
            }

            // ----- INSERT × slots.length (REQ_ID 는 PK 단일 컬럼이므로 slot 마다 새로 채번) -----
            for (SlotRequest s : param.slots()) {
                String slotReqId = mapper.selectNextReqId(param.cmpnyCd());
                if (reqId == null) reqId = slotReqId;
                AttdReqInsertCommand cmd = new AttdReqInsertCommand(
                        slotReqId,
                        param.cmpnyCd(), param.siteCd(), param.userCd(),
                        AttdReqTypeUtils.REQ_TYPE_OT_REGISTER,
                        null,                            // TARGET_ID (생성 요청)
                        REQ_STATUS_REQUESTED,
                        param.reqReason(),
                        param.workYmd(), param.nodeCd(),
                        s.getWorkSeq(),
                        s.getStartDate(), s.getStartTime(),
                        s.getEndDate(), s.getEndTime(),
                        null,                            // SCH_CD
                        param.userCd()
                );
                mapper.insertAttdReq(cmd);
                // prafta-app-009: 슬롯(REQ_ID)마다 결재 분기/라인 처리(같은 @Transactional).
                attdApprovalLineService.applyApprovalFlow(
                        param.cmpnyCd(), param.siteCd(), param.userCd(), slotReqId,
                        param.approverUserCds(), param.presetId(), param.userCd());
                workSeqs.add(s.getWorkSeq());
            }
        } finally {
            releaseDupLock(lockKey);    // (2) 타입별 락 해제(획득 역순)
        }
        } finally {
            releaseDupLock(mutexKey);   // (1) 공통 mutex 해제
        }

        log.info("[prafta-app-007] 초과근무 신청 등록 — reqId={}, userCd={}, workYmd={}, slots={}",
                reqId, param.userCd(), param.workYmd(), workSeqs.size());

        return new RegisterReqResponse(
                reqId,
                AttdReqTypeUtils.REQ_TYPE_OT_REGISTER,
                REQ_STATUS_REQUESTED,
                workSeqs);
    }

    // ============================================================
    // 4) 스케줄 선택 옵션 목록 (GET /appApi/req07/schedules)
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public SchedOptionResponse getSchedOptions(String cmpnyCd, String siteCd) {
        List<SchedOptionResult> schedules = mapper.selectSchedOptions(cmpnyCd, siteCd);
        if (schedules == null) {
            schedules = new ArrayList<>();
        }
        log.info("[prafta-app-007] 스케줄 옵션 조회 — cmpnyCd={}, siteCd={}, count={}",
                cmpnyCd, siteCd, schedules.size());
        return new SchedOptionResponse(schedules);
    }

    // ============================================================
    // prafta-app-009 가드/락 헬퍼 (private — 모듈 내부 한정)
    // ============================================================

    /**
     * F12 마감 가드: 해당 근무월(YYYYMM)이 사용자 소속 부서 마감 커버리지에 포함되면 거부(ATTD_400_099).
     * 스케줄 수정은 미래도 마감 대상일 수 있으므로 일자 무관(과거/미래 모두)으로 적용한다.
     * web {@code AttdCloseService.isClosedForUser} 재사용(부서 단위 PRAFTA-028 정밀판정).
     */
    private void assertNotClosed(String cmpnyCd, String siteCd, String userCd, String workYmd) {
        if (workYmd == null || workYmd.length() < 6) {
            return; // 형식 이상은 상위 구조검증에서 처리(여기선 no-op).
        }
        String closeYm = workYmd.substring(0, 6);
        if (attdCloseService.isClosedForUser(cmpnyCd, siteCd, userCd, closeYm)) {
            log.info("[prafta-app-009] 마감 가드 거부 — userCd={}, closeYm={}", userCd, closeYm);
            throw new ApiException(AttdErrorCode.ATTD_400_099);
        }
    }

    /**
     * F13 스케줄 존재 가드: 본인 근무계획 행이 없는 일자(미배정)면 거부(ATTD_400_098).
     * 스케줄 수정/근태 보정은 배정된 근무일 대상으로만 요청 가능.
     */
    private void assertWorkPlanExists(String cmpnyCd, String siteCd, String userCd, String workYmd) {
        int cnt = mapper.countUserWorkPlan(cmpnyCd, siteCd, userCd, workYmd);
        if (cnt <= 0) {
            log.info("[prafta-app-009] 스케줄 존재 가드 거부 — userCd={}, workYmd={}", userCd, workYmd);
            throw new ApiException(AttdErrorCode.ATTD_400_098);
        }
    }

    /** F15 advisory lock 키: 중복 차단 단위(회사+사업장+사용자+일자+요청유형)로 직렬화. */
    private String dupLockKey(String cmpnyCd, String siteCd, String userCd, String workYmd, String reqType) {
        return "ATTD_REQ:" + cmpnyCd + ":" + siteCd + ":" + userCd + ":" + workYmd + ":" + reqType;
    }

    /**
     * PRAFTA-APP-022 룰A 상호배제용 <b>공통</b> advisory lock 키: 요청유형을 <b>제외</b>(회사+사업장+사용자+일자).
     *
     * <p>타입별 {@link #dupLockKey}(OT='03' vs 스케줄수정='10')는 서로 다른 락이라, 같은 cmpny/site/user/workYmd
     * 에 초과근무와 스케줄수정이 거의 동시에 제출되면 두 트랜잭션이 각자 다른 락만 잡고 병렬 진행하여
     * 상호배제 카운트(countActiveOvertimeReq / countActiveSchedModify)를 INSERT 커밋 전 0으로 읽어 둘 다 등록되는
     * TOCTOU 갭이 있었다. 이를 막기 위해 {@code registerOvertime}/{@code registerSchedModify} <b>양쪽</b>이 동일한
     * 이 키를 1개 더 잡아 cross-type 제출을 직렬화한다. 획득/해제는 dupLock 과 동일 유틸(GET_LOCK/RELEASE_LOCK) 재사용.
     */
    private String ruleAMutexKey(String cmpnyCd, String siteCd, String userCd, String workYmd) {
        return "ATTD_REQ_MUTEX:" + cmpnyCd + ":" + siteCd + ":" + userCd + ":" + workYmd;
    }

    /**
     * F15 advisory lock 획득. 타임아웃/오류면 동시 처리로 보고 ATTD_400_090(중복 요청)으로 변환.
     * (락을 못 잡았다는 것은 동일 키의 다른 제출이 진행 중이라는 뜻 — 사용자에게는 중복 요청 안내.)
     */
    private void acquireDupLock(String lockKey) {
        Integer got = mapper.getAdvisoryLock(lockKey, DUP_LOCK_TIMEOUT_SEC);
        if (got == null || got != 1) {
            log.info("[prafta-app-009] 중복차단 advisory lock 미획득 — lockKey={}, got={}", lockKey, got);
            throw new ApiException(AttdErrorCode.ATTD_400_090);
        }
    }

    /** F15 advisory lock 해제(예외 무시 — 세션 종료 시 자동 해제됨). */
    private void releaseDupLock(String lockKey) {
        try {
            mapper.releaseAdvisoryLock(lockKey);
        } catch (Exception e) {
            log.warn("[prafta-app-009] 중복차단 advisory lock 해제 실패(무시) — lockKey={}", lockKey, e);
        }
    }

    // ============================================================
    // 공통 검증 헬퍼 (private — 모듈 내부 한정)
    // ============================================================

    /** slots 배열 크기 검증 (1 또는 2). */
    private void validateSlotsSize(List<SlotRequest> slots) {
        if (slots == null || slots.isEmpty() || slots.size() > 2) {
            throw new ApiException(AttdErrorCode.ATTD_400_091);
        }
    }

    /** workSeq 중복 금지 (1 / 2 만 허용, 같은 번호 두 번 금지). */
    private void validateNoDuplicateWorkSeq(List<SlotRequest> slots) {
        Set<Integer> seen = new HashSet<>(slots.size());
        for (SlotRequest s : slots) {
            Integer ws = s.getWorkSeq();
            if (ws == null || (ws != 1 && ws != 2)) {
                throw new ApiException(AttdErrorCode.ATTD_400_091);
            }
            if (!seen.add(ws)) {
                throw new ApiException(AttdErrorCode.ATTD_400_092);
            }
        }
    }

    /**
     * 시각 입력 검증 (근태 보정 / 초과근무 공통):
     * <ul>
     *   <li>HHmm 4자리 형식 (00:00 ~ 23:59) — 4자 숫자, hh<24, mm<60.</li>
     *   <li>start < end (자정 넘김 시 endDate 가 +1 일이면 허용).</li>
     *   <li>2구간이면 slot1.end ≤ slot2.start (자정 보정은 단순 비교 — startDate/endDate 기준).</li>
     * </ul>
     */
    private void validateSlotsTimes(List<SlotRequest> slots) {
        // 1) 각 slot 자체 검증
        long[][] absMin = new long[slots.size()][2]; // [i][0]=start_abs_min, [i][1]=end_abs_min
        for (int i = 0; i < slots.size(); i++) {
            SlotRequest s = slots.get(i);
            int sMin = parseHHmm(s.getStartTime());
            int eMin = parseHHmm(s.getEndTime());
            if (sMin < 0 || eMin < 0
                    || !StringUtils.hasText(s.getStartDate())
                    || !StringUtils.hasText(s.getEndDate())) {
                throw new ApiException(AttdErrorCode.ATTD_400_093);
            }
            long sAbs = ymdToDays(s.getStartDate()) * 1440L + sMin;
            long eAbs = ymdToDays(s.getEndDate()) * 1440L + eMin;
            if (eAbs <= sAbs) {
                throw new ApiException(AttdErrorCode.ATTD_400_093);
            }
            absMin[i][0] = sAbs;
            absMin[i][1] = eAbs;
        }

        // 2) 2구간 시 겹침 금지 (slot1.end ≤ slot2.start)
        if (slots.size() == 2) {
            // workSeq 기준 정렬: 1구간 먼저, 2구간 다음
            int firstIdx = slots.get(0).getWorkSeq() == 1 ? 0 : 1;
            int secondIdx = 1 - firstIdx;
            if (absMin[firstIdx][1] > absMin[secondIdx][0]) {
                throw new ApiException(AttdErrorCode.ATTD_400_094);
            }
        }
    }

    /**
     * prafta-app-019(1-A/1-B): OT 슬롯이 해당 구간의 실제 근태기록 [CHECK_IN~CHECK_OUT] 범위 안에
     * 포함되는지 검증한다(차집합 = 실근태 − 스케줄 안에서만 OT 허용). 미포함/근태부재/미퇴근이면 거부(ATTD_400_104).
     *
     * <ul>
     *   <li>1-A 구간별 매칭: slot.workSeq 에 대응하는 그 구간(WORK_SEQ)의 실근태 1건으로 검증(통합범위 아님).</li>
     *   <li>1-B: 실근태 행이 없거나 CHECK_OUT_TIME 이 공백/null(체크아웃 미완료) → 범위 확정 불가 → 거부.</li>
     *   <li>시각 기준은 원본 CHECK_IN_TIME/CHECK_OUT_TIME(표준화 적용시각 아님).</li>
     *   <li>인스턴트 환산은 ymdToDays(date)*1440 + parseHHmm(time)(assertNoScheduleOverlap 과 동일 유틸).
     *       자정 넘김은 CHECK_OUT_DATE/CHECK_IN_DATE 를 그대로 사용.</li>
     *   <li>실근태 out ≤ in(데이터 이상) → 범위 신뢰 불가 → 거부.</li>
     *   <li>포함 조건: attdIn ≤ otStart && otEnd ≤ attdOut(경계 일치 통과 — ≤).</li>
     * </ul>
     *
     * <p>OT 측(otStart/otEnd)은 validateSlotsTimes 통과분이므로 형식·순서 안전.
     */
    private void assertWithinActualAttdWindow(String cmpnyCd, String siteCd, String userCd,
                                              String workYmd, SlotRequest slot) {
        ActualAttdWindowResult attd = mapper.selectActualAttdWindowBySlot(
                cmpnyCd, siteCd, userCd, workYmd, slot.getWorkSeq());

        // 1-B: 실근태 행 부재 또는 체크아웃 미완료(시각 공백/null) → 범위 확정 불가 → 거부.
        if (attd == null
                || !StringUtils.hasText(attd.checkInTime())
                || !StringUtils.hasText(attd.checkOutTime())) {
            log.info("[prafta-app-019] OT 실근태 범위 확정불가 거부 — userCd={}, workYmd={}, workSeq={}, attdNull={}",
                    userCd, workYmd, slot.getWorkSeq(), (attd == null));
            throw new ApiException(AttdErrorCode.ATTD_400_104);
        }

        // 실근태 시각 환산(원본 CHECK_IN/OUT). CHECK_OUT_DATE 가 공백이면 CHECK_IN_DATE 로 폴백(자정 미넘김 가정).
        String outDate = StringUtils.hasText(attd.checkOutDate()) ? attd.checkOutDate() : attd.checkInDate();
        long attdIn = ymdToDays(attd.checkInDate()) * 1440L + parseHHmm(attd.checkInTime());
        long attdOut = ymdToDays(outDate) * 1440L + parseHHmm(attd.checkOutTime());

        // 실근태 시각 파싱 실패(예외 데이터) 또는 out ≤ in(범위 신뢰 불가) → 거부(fail-closed).
        if (parseHHmm(attd.checkInTime()) < 0 || parseHHmm(attd.checkOutTime()) < 0 || attdOut <= attdIn) {
            log.info("[prafta-app-019] OT 실근태 범위 이상 거부 — userCd={}, workYmd={}, workSeq={}, attd=[{} {}~{} {}]",
                    userCd, workYmd, slot.getWorkSeq(),
                    attd.checkInDate(), attd.checkInTime(), outDate, attd.checkOutTime());
            throw new ApiException(AttdErrorCode.ATTD_400_104);
        }

        // OT 슬롯 환산(validateSlotsTimes 통과분).
        long otStart = ymdToDays(slot.getStartDate()) * 1440L + parseHHmm(slot.getStartTime());
        long otEnd = ymdToDays(slot.getEndDate()) * 1440L + parseHHmm(slot.getEndTime());

        // 포함 조건: attdIn ≤ otStart && otEnd ≤ attdOut. 벗어나면 거부.
        if (!(attdIn <= otStart && otEnd <= attdOut)) {
            log.info("[prafta-app-019] OT 실근태범위 이탈 거부 — userCd={}, workYmd={}, workSeq={}, ot=[{}~{}], attd=[{}~{}]",
                    userCd, workYmd, slot.getWorkSeq(),
                    slot.getStartTime(), slot.getEndTime(), attd.checkInTime(), attd.checkOutTime());
            throw new ApiException(AttdErrorCode.ATTD_400_104);
        }
    }

    /**
     * prafta-app-017(이슈①): OT 슬롯이 해당 구간 정규 스케줄과 겹치면 거부(ATTD_400_100).
     *
     * <ul>
     *   <li>스케줄 행 자체가 없으면(연차/NULL/미존재) 정규구간 부재 → 면제(전량 OT 허용).</li>
     *   <li>slot.workSeq==1 → FST*, ==2 → SEC* 와 비교. 해당 구간 시각이 공백이면 그 구간 면제.</li>
     *   <li>스케줄 시각 파싱 실패(예외적 데이터)면 그 구간 정규구간 판정 불가 →
     *       겹침검사 면제(WARN 로그). 데이터 오류로 정상요청을 막지 않는다(스케줄 행 부재와 구분 로그).</li>
     *   <li>인스턴트 비교: stamp = epochDay*1440 + 분. 스케줄 종료 ≤ 시작이면 익일 보정(+1440).</li>
     *   <li>겹침 조건: otStart &lt; schEnd && schStart &lt; otEnd (접함은 겹침 아님 → 허용).</li>
     * </ul>
     *
     * <p>OT 측(otStart/otEnd)은 validateSlotsTimes 통과분이므로 형식·순서 안전.
     */
    private void assertNoScheduleOverlap(String workYmd, ScheduleWindowResult schedule,
                                         SlotRequest slot, String userCd) {
        // 스케줄 행 부재(연차/NULL/미매칭) → 정규구간 없음 → 면제.
        if (schedule == null) {
            return;
        }

        // 구간 매핑(workSeq 기준).
        String schStrTime;
        String schEndTime;
        if (slot.getWorkSeq() != null && slot.getWorkSeq() == 2) {
            schStrTime = schedule.secStrTime();
            schEndTime = schedule.secEndTime();
        } else {
            schStrTime = schedule.fstStrTime();
            schEndTime = schedule.fstEndTime();
        }

        // 해당 구간 시각 공백(예: 1구간 전용 스케줄에 2구간 OT) → 정규구간 없음 → 면제.
        if (!StringUtils.hasText(schStrTime) || !StringUtils.hasText(schEndTime)) {
            return;
        }

        int schStrMin = parseHHmm(schStrTime);
        int schEndMin = parseHHmm(schEndTime);
        // 스케줄 시각 파싱 실패(예외적 데이터) → 겹침검사 면제(fail-open, WARN). 스케줄 행 부재와 구분.
        if (schStrMin < 0 || schEndMin < 0) {
            log.warn("[prafta-app-017] OT 겹침검사 스케줄 시각 파싱 실패(면제) — userCd={}, workYmd={}, workSeq={}, schCd={}, str={}, end={}",
                    userCd, workYmd, slot.getWorkSeq(), schedule.schCd(), schStrTime, schEndTime);
            return;
        }

        long base = ymdToDays(workYmd) * 1440L;
        long schStart = base + schStrMin;
        long schEnd = base + schEndMin;
        // 종료 ≤ 시작 → 야간/자정 넘김 → 익일 보정.
        if (schEnd <= schStart) {
            schEnd += 1440L;
        }

        long otStart = ymdToDays(slot.getStartDate()) * 1440L + parseHHmm(slot.getStartTime());
        long otEnd = ymdToDays(slot.getEndDate()) * 1440L + parseHHmm(slot.getEndTime());

        // 겹침: otStart < schEnd && schStart < otEnd (접함 허용).
        if (otStart < schEnd && schStart < otEnd) {
            log.info("[prafta-app-017] OT 스케줄 겹침 거부 — userCd={}, workYmd={}, workSeq={}, ot=[{}~{}], sch=[{}~{}]",
                    userCd, workYmd, slot.getWorkSeq(),
                    slot.getStartTime(), slot.getEndTime(), schStrTime, schEndTime);
            throw new ApiException(AttdErrorCode.ATTD_400_100);
        }
    }

    /**
     * PRAFTA-APP-022 룰C: 오늘 일자(yyyyMMdd) 문자열. WORK_YMD(varchar8) 문자열 비교용.
     * (leaveflow AppLeaveFlowServiceImpl.todayYmd 와 동일 패턴 — 서버 로컬 일자 기준.)
     */
    private String todayYmd() {
        java.time.LocalDate d = java.time.LocalDate.now();
        return String.format("%04d%02d%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    /** HHmm 문자열 → 분 단위 정수. 형식 위반 시 -1. */
    private int parseHHmm(String hhmm) {
        if (hhmm == null || hhmm.length() != 4) return -1;
        for (int i = 0; i < 4; i++) {
            char c = hhmm.charAt(i);
            if (c < '0' || c > '9') return -1;
        }
        int h = Integer.parseInt(hhmm.substring(0, 2));
        int m = Integer.parseInt(hhmm.substring(2, 4));
        if (h < 0 || h > 23 || m < 0 || m > 59) return -1;
        return h * 60 + m;
    }

    /**
     * YYYYMMDD 문자열을 1970-01-01 기준 epoch-day(정수)로 매핑한다.
     * {@link java.time.LocalDate#toEpochDay()} 기반이라 월/연 경계와 자정 넘김에서도 전역으로
     * 단조 증가한다(같은 달 가정 불필요). 시각과 결합해 {@code ymdToDays(ymd)*1440 + 분} 형태의
     * (일자+시각) 인스턴트 비교에 사용한다 — validateSlotsTimes 의 absMin, assertNoScheduleOverlap 의
     * 겹침 판정이 모두 이 함수에 의존하므로 단순 정수(year*10000+...)로 바꾸지 말 것(경계 오판 발생).
     * 파싱 불가(형식 위반)면 0L 반환.
     */
    private long ymdToDays(String ymd) {
        if (ymd == null || ymd.length() != 8) return 0L;
        try {
            int y = Integer.parseInt(ymd.substring(0, 4));
            int m = Integer.parseInt(ymd.substring(4, 6));
            int d = Integer.parseInt(ymd.substring(6, 8));
            return java.time.LocalDate.of(y, m, d).toEpochDay();
        } catch (Exception e) {
            return 0L;
        }
    }
}
