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
import com.prafta.app.req.req07.mapper.AppReq07Mapper;
import com.prafta.app.req.req07.service.AppReq07Service;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
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

    /** REQ_STATUS = '01' 신청 (등록 직후 고정 — P3, Q3 결재선 미포함). */
    private static final String REQ_STATUS_REQUESTED = AttdReqTypeUtils.REQ_STATUS_REQUESTED;

    /** OT_TYPE allow-list (P12). */
    private static final Set<String> ALLOWED_OT_TYPES = Set.of("EXTEND", "NIGHT", "HOLIDAY");

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

        // ----- 중복 요청 차단 (P10) -----
        int dup = mapper.countDuplicateReq(
                param.cmpnyCd(), param.siteCd(), param.userCd(),
                param.workYmd(), AttdReqTypeUtils.REQ_TYPE_SCHED_MODIFY);
        if (dup > 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_090);
        }

        // ----- REQ_ID 채번 -----
        String reqId = mapper.selectNextReqId(param.cmpnyCd());

        // ----- INSERT × slots.length -----
        List<Integer> workSeqs = new ArrayList<>(param.slots().size());
        for (SlotRequest s : param.slots()) {
            AttdReqInsertCommand cmd = new AttdReqInsertCommand(
                    reqId,
                    param.cmpnyCd(), param.siteCd(), param.userCd(),
                    AttdReqTypeUtils.REQ_TYPE_SCHED_MODIFY,
                    null,                            // TARGET_ID (스케줄 수정은 null)
                    REQ_STATUS_REQUESTED,
                    param.reqReason(),
                    param.workYmd(), param.nodeCd(),
                    s.getWorkSeq(),
                    null, null, null, null,          // START/END_DATE/TIME (스케줄 수정은 null)
                    null,                            // OT_TYPE
                    s.getSchCd(),                    // SCH_CD (REQ_TYPE='10' 전용)
                    param.userCd()                   // INSERT_NO
            );
            mapper.insertAttdReq(cmd);
            workSeqs.add(s.getWorkSeq());
        }

        log.info("[prafta-app-007] 스케줄 수정 요청 등록 — reqId={}, userCd={}, workYmd={}, slots={}",
                reqId, param.userCd(), param.workYmd(), workSeqs.size());

        // TODO(prafta-app-009): tb_user_attd_req_approval INSERT (결재선 통합)
        // TODO(prafta-031): tb_noti_outbox INSERT (관리자 push 알림)

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

        // ----- 자동 분기 (Q2) — slot 별 사전 조회 -----
        // 트랜잭션 시작 직후 일괄 조회. INSERT 중간에 다른 트랜잭션이 ATTD 행을 만들 가능성은
        // 본 1차 모델에서 무시 (다음 처리 단계에서 관리자 승인 시 재검증).
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

        // ----- REQ_ID 채번 -----
        String reqId = mapper.selectNextReqId(param.cmpnyCd());

        // ----- INSERT × slots.length -----
        List<Integer> workSeqs = new ArrayList<>(param.slots().size());
        for (int i = 0; i < param.slots().size(); i++) {
            SlotRequest s = param.slots().get(i);
            AttdReqInsertCommand cmd = new AttdReqInsertCommand(
                    reqId,
                    param.cmpnyCd(), param.siteCd(), param.userCd(),
                    reqTypes.get(i),
                    targetIds.get(i),
                    REQ_STATUS_REQUESTED,
                    param.reqReason(),
                    param.workYmd(), param.nodeCd(),
                    s.getWorkSeq(),
                    s.getStartDate(), s.getStartTime(),
                    s.getEndDate(), s.getEndTime(),
                    null,                            // OT_TYPE (보정 미사용)
                    null,                            // SCH_CD (보정 미사용)
                    param.userCd()
            );
            mapper.insertAttdReq(cmd);
            workSeqs.add(s.getWorkSeq());
        }

        // 응답 reqType: 단일 슬롯이면 해당 REQ_TYPE, 1구간 행 존재 + 2구간 부재 등 다른 케이스 → 'MIXED'
        String responseReqType;
        if (hasCreate && hasModify) {
            responseReqType = "MIXED";
        } else if (hasModify) {
            responseReqType = AttdReqTypeUtils.REQ_TYPE_ATTD_MODIFY;
        } else {
            responseReqType = AttdReqTypeUtils.REQ_TYPE_ATTD_CREATE;
        }

        log.info("[prafta-app-007] 근태 보정 요청 등록 — reqId={}, userCd={}, workYmd={}, slots={}, reqType={}",
                reqId, param.userCd(), param.workYmd(), workSeqs.size(), responseReqType);

        // TODO(prafta-app-009): tb_user_attd_req_approval INSERT
        // TODO(prafta-031): tb_noti_outbox INSERT

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

        // OT_TYPE allow-list (P12)
        for (SlotRequest s : param.slots()) {
            String otType = s.getOtType();
            if (otType == null || !ALLOWED_OT_TYPES.contains(otType)) {
                throw new ApiException(AttdErrorCode.ATTD_400_095);
            }
        }

        // ----- 중복 요청 차단 (P10) -----
        int dup = mapper.countDuplicateReq(
                param.cmpnyCd(), param.siteCd(), param.userCd(),
                param.workYmd(), AttdReqTypeUtils.REQ_TYPE_OT_REGISTER);
        if (dup > 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_090);
        }

        // ----- REQ_ID 채번 -----
        String reqId = mapper.selectNextReqId(param.cmpnyCd());

        // ----- INSERT × slots.length -----
        List<Integer> workSeqs = new ArrayList<>(param.slots().size());
        for (SlotRequest s : param.slots()) {
            AttdReqInsertCommand cmd = new AttdReqInsertCommand(
                    reqId,
                    param.cmpnyCd(), param.siteCd(), param.userCd(),
                    AttdReqTypeUtils.REQ_TYPE_OT_REGISTER,
                    null,                            // TARGET_ID (생성 요청)
                    REQ_STATUS_REQUESTED,
                    param.reqReason(),
                    param.workYmd(), param.nodeCd(),
                    s.getWorkSeq(),
                    s.getStartDate(), s.getStartTime(),
                    s.getEndDate(), s.getEndTime(),
                    s.getOtType(),
                    null,                            // SCH_CD
                    param.userCd()
            );
            mapper.insertAttdReq(cmd);
            workSeqs.add(s.getWorkSeq());
        }

        log.info("[prafta-app-007] 초과근무 신청 등록 — reqId={}, userCd={}, workYmd={}, slots={}",
                reqId, param.userCd(), param.workYmd(), workSeqs.size());

        // TODO(prafta-app-009): tb_user_attd_req_approval INSERT
        // TODO(prafta-031): tb_noti_outbox INSERT

        return new RegisterReqResponse(
                reqId,
                AttdReqTypeUtils.REQ_TYPE_OT_REGISTER,
                REQ_STATUS_REQUESTED,
                workSeqs);
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
     * YYYYMMDD 문자열을 epoch-day 와 무관한 단순 정수로 매핑 (비교 전용).
     * year*10000 + month*100 + day 는 월 경계에서 일수 차가 비선형이지만,
     * 같은 달 안에서는 단조 증가. 자정 넘김 보정은 endDate = workYmd 또는 +1 일만 1차로 허용.
     * 본 함수는 동등 비교 + workYmd vs workYmd+1 의 1일 차이만 사용한다 (Service 의 absMin 계산은 같은 달
     * 가정 — 자정 넘김의 실제 day-of-week 산출은 follow-up 단계에서 정밀화).
     *
     * <p>1차 단순 처리 (plan P9 "자정 보정 단순"). 월 경계에서 잘못된 차이가 나도
     * end ≤ start 검증과 ATTD_400_093 메시지로 사용자가 인지 가능.
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
