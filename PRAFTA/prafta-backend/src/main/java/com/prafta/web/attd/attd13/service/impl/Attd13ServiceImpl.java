package com.prafta.web.attd.attd13.service.impl;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.service.LeaveHourlyResettleService;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.attd.attd13.application.command.LeaveChangeRequestInsertCommand;
import com.prafta.web.attd.attd13.application.param.ChangeRequestConfirmParam;
import com.prafta.web.attd.attd13.application.param.ChangeRequestCreateParam;
import com.prafta.web.attd.attd13.application.param.ChangeRequestListParam;
import com.prafta.web.attd.attd13.application.param.ChangeRequestRejectParam;
import com.prafta.web.attd.attd13.mapper.Attd13Mapper;
import com.prafta.web.attd.attd13.result.LeaveChangeRequestRowResult;
import com.prafta.web.attd.attd13.result.LeaveUseTargetResult;
import com.prafta.web.attd.attd13.result.MovableLeaveResult;
import com.prafta.web.attd.attd13.service.Attd13Service;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.leaveflow.mapper.LeaveFlowMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link Attd13Service} 구현 (PRAFTA-COM-008-C).
 *
 * <p>정책서: prafta-com-008-C-use-source-consent.md §3(흐름) / §5(권한) / §6(PUSH) / §7(엣지).
 * GRANT_ID 미분할(§0): 일자별 식별은 LEAVE_ID, 차감 동기화는 recomputeGrantUsedDays 재사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Attd13ServiceImpl implements Attd13Service {

    // 상태/코드 상수
    private static final String INITIATOR_ADMIN = "ADMIN";
    private static final String INITIATOR_WORKER = "WORKER";
    private static final String REQ_TYPE_MOVE = "MOVE";
    private static final String REQ_TYPE_DELETE = "DELETE";

    private static final String STATUS_REQUESTED = "REQUESTED";
    private static final String STATUS_AGREED = "AGREED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final String RESPONSE_AGREE = "AGREE";
    private static final String RESPONSE_REJECT = "REJECT";

    private static final String LEAVE_STATUS_CONFIRMED = "CONFIRMED";
    private static final String UNIT_FULL = "00"; // 일 단위(출근 차단 블록 대상)
    private static final String WHOLE_SITE = "*"; // 전체 부서 스코프(노드 관리자는 사용 불가)

    private static final String CHANNEL_PUSH = "PUSH";
    private static final String SEND_STATUS_PENDING = "PENDING";
    private static final String NOTI_REQUEST = "LEAVE_CHANGE_REQUEST";
    private static final String NOTI_RESPONSE = "LEAVE_CHANGE_RESPONSE";
    private static final String NOTI_CONFIRMED = "LEAVE_CHANGE_CONFIRMED";
    private static final String NOTI_REJECTED = "LEAVE_CHANGE_REJECTED";

    private static final String CANCEL_REASON_CHANGE_DELETE = "관리자 연차 삭제(동의 확정)";

    private final Attd13Mapper attd13Mapper;
    private final AttdCloseService attdCloseService;
    private final LeaveFlowMapper leaveFlowMapper;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;
    /** LC-05(F1): 시간차 행 삭제 시 그날 잔존 시간차 건 시간순 재정산(코어 산식 LC-03 공유). */
    private final LeaveHourlyResettleService leaveHourlyResettleService;

    // ============================================================
    // 관리자(웹)
    // ============================================================

    @Override
    public List<LeaveChangeRequestRowResult> getChangeRequests(ChangeRequestListParam param) {
        // 역할 기반 스코프(작업1 D1+D3):
        //   - master/hr: 회사 전사(사업장 미지정=전체, 지정 시 해당 사업장). 부서 미지정 허용.
        //   - 노드 정·부 관리자: 본인 담당 노드(+하위) 강제. 부서 미지정 진입은 403 대신 안내성 BadRequest.
        boolean siteWide = AuthRoleUtils.isManager(param.gvAuthCd());
        if (!siteWide) {
            // 노드 관리자는 부서 필수(미지정 시 즉시 403 대신 안내). 빈 nodeCd 로 조회하면 ensureCanManageScope 가 403.
            if (param.nodeCd() == null || param.nodeCd().isBlank() || WHOLE_SITE.equals(param.nodeCd())) {
                throw new ApiException(AttdErrorCode.ATTD_400_130);
            }
            // 지정 노드에 대한 관리 권한(해당/상위 부서 정·부 관리자) 강제(safe 제외)
            ensureCanManageScope(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd());
        }
        return attd13Mapper.selectChangeRequests(
                param.gvCmpnyCd(), siteWide ? "Y" : "N", param.siteCd(), param.nodeCd(), param.incSubNodeYn(),
                param.userNm(), param.reqStatus());
    }

    @Override
    public LeaveChangeRequestRowResult getChangeRequestDetail(String cmpnyCd, String authCd, String userCd, String changeReqId) {
        LeaveChangeRequestRowResult req = attd13Mapper.selectChangeRequest(cmpnyCd, changeReqId);
        if (req == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_121);
        }
        // 상세 열람도 대상자 관리 권한 강제(작업1 — safe 제외)
        ensureCanManageUser(authCd, userCd, cmpnyCd, req.siteCd(), req.targetUserCd());
        return req;
    }

    @Override
    @Transactional
    public void createChangeRequest(ChangeRequestCreateParam param) {
        // 1) 대상 연차 재조회(IDOR/스코프 fail-closed)
        LeaveUseTargetResult target = loadConfirmedTarget(param.gvCmpnyCd(), param.targetLeaveId());

        // 2) 대상 근로자 관리 권한(master/hr/safe 또는 대상자 소속/상위 부서 정·부 관리자)
        ensureCanManageUser(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), target.siteCd(), target.userCd());

        // 3) 마감 가드(§3-2-1): 대상일(출발일) + 이동 대상일(MOVE 시) 양쪽 검사
        ensureNotClosed(param.gvCmpnyCd(), target.siteCd(), target.userCd(), target.startDate());
        if (REQ_TYPE_MOVE.equals(param.reqType())) {
            ensureNotClosed(param.gvCmpnyCd(), target.siteCd(), target.userCd(), param.moveTargetDate());
            // 이동 검증(만료 이내 + DIRECT_USE_KEY 충돌)
            validateMove(param.gvCmpnyCd(), target, param.moveTargetDate());
        }

        // 4) REQUESTED 생성(활성요청 멱등 — UNIQUE 위반 시 변환)
        String changeReqId = insertRequest(param.gvCmpnyCd(), target.siteCd(), target.userCd(), target.leaveId(),
                INITIATOR_ADMIN, param.reqType(), param.moveTargetDate(), param.reqReason(), param.gvUserCd());
        // (관리자 발의는 근로자 응답 단계가 필요하므로 REQUESTED 유지)

        // 5) 근로자 PUSH 적재(요청 통지). 예외 격리(본 흐름 영향 금지).
        String body = REQ_TYPE_MOVE.equals(param.reqType())
                ? "연차 변경(이동) 요청이 도착했어요. 동의 또는 거부해 주세요."
                : "연차 삭제 요청이 도착했어요. 동의 또는 거부해 주세요.";
        enqueuePush(param.gvCmpnyCd(), target.siteCd(), target.userCd(), NOTI_REQUEST, "연차 변경 요청", body, changeReqId, param.gvUserCd());

        log.info("관리자 연차 변경 발의. cmpnyCd={}, leaveId={}, reqType={}, by={}",
                param.gvCmpnyCd(), target.leaveId(), param.reqType(), param.gvUserCd());
    }

    @Override
    @Transactional
    public void confirmChangeRequest(ChangeRequestConfirmParam param) {
        // 1) 요청 재조회
        LeaveChangeRequestRowResult req = attd13Mapper.selectChangeRequest(param.gvCmpnyCd(), param.changeReqId());
        if (req == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_121);
        }
        // 2) AGREED 만 확인 가능
        if (!STATUS_AGREED.equals(req.reqStatus())) {
            throw new ApiException(AttdErrorCode.ATTD_409_120);
        }
        // 3) 대상 연차 재조회(반영 시점 정합 — 확정/미삭제만). 권한·반영 대상 일원화(M1).
        LeaveUseTargetResult target = loadConfirmedTarget(param.gvCmpnyCd(), req.targetLeaveId());

        // 4) 대상 근로자 관리 권한 재검증 — 요청행이 아닌 반영 대상(target)의 siteCd/userCd 기준(M1)
        ensureCanManageUser(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), target.siteCd(), target.userCd());

        // 5) 마감 재가드(확인 시점 기준)
        ensureNotClosed(param.gvCmpnyCd(), target.siteCd(), target.userCd(), target.startDate());
        if (REQ_TYPE_MOVE.equals(req.reqType())) {
            ensureNotClosed(param.gvCmpnyCd(), target.siteCd(), target.userCd(), req.moveTargetDate());
            validateMove(param.gvCmpnyCd(), target, req.moveTargetDate());
        }

        // 6) 상태 전이(AGREED → CONFIRMED). 경합 시 0행 → 충돌
        if (attd13Mapper.confirmChangeRequest(param.gvCmpnyCd(), param.changeReqId(), param.gvUserCd()) == 0) {
            throw new ApiException(AttdErrorCode.ATTD_409_120);
        }

        // 7) 실제 반영
        if (REQ_TYPE_MOVE.equals(req.reqType())) {
            applyMove(param.gvCmpnyCd(), target, req.moveTargetDate(), param.gvUserCd());
        } else {
            applyDelete(param.gvCmpnyCd(), target, param.gvUserCd());
        }

        // 8) 근로자 PUSH 적재(확인 결과)
        String body = REQ_TYPE_MOVE.equals(req.reqType())
                ? "연차 이동이 확정되었어요."
                : "연차 삭제가 확정되었어요. 해당 일자는 근무일로 복귀합니다.";
        enqueuePush(param.gvCmpnyCd(), target.siteCd(), target.userCd(), NOTI_CONFIRMED, "연차 변경 확인", body, param.changeReqId(), param.gvUserCd());

        log.info("관리자 연차 변경 확인 반영. cmpnyCd={}, changeReqId={}, reqType={}, by={}",
                param.gvCmpnyCd(), param.changeReqId(), req.reqType(), param.gvUserCd());
    }

    @Override
    @Transactional
    public void rejectChangeRequest(ChangeRequestRejectParam param) {
        // 1) 요청 재조회
        LeaveChangeRequestRowResult req = attd13Mapper.selectChangeRequest(param.gvCmpnyCd(), param.changeReqId());
        if (req == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_121);
        }
        // 2) AGREED 만 반려 가능(주로 WORKER 발의=생성즉시 AGREED). 멱등 — 이미 REJECTED/CONFIRMED 면 409.
        if (!STATUS_AGREED.equals(req.reqStatus())) {
            throw new ApiException(AttdErrorCode.ATTD_409_120);
        }
        // 3) 대상 연차 재조회 후 그 siteCd/userCd 기준으로 권한 검증(M1 — confirm 과 동일 기준 통일).
        //    반려는 연차를 반영하지 않으나 권한 대상 일원화로 일관성 유지(safe 제외).
        LeaveUseTargetResult target = loadConfirmedTarget(param.gvCmpnyCd(), req.targetLeaveId());
        ensureCanManageUser(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), target.siteCd(), target.userCd());

        // 4) 상태 전이(AGREED → REJECTED). 원 연차 불변. 경합 시 0행 → 충돌.
        if (attd13Mapper.rejectChangeRequest(
                param.gvCmpnyCd(), param.changeReqId(), param.rejectReason(), param.gvUserCd()) == 0) {
            throw new ApiException(AttdErrorCode.ATTD_409_120);
        }

        // 5) 근로자(발의자) PUSH 적재(반려 결과). WORKER 발의건이면 발의자=대상 근로자.
        enqueuePush(param.gvCmpnyCd(), req.siteCd(), req.initiatorUserCd(), NOTI_REJECTED,
                "연차 변경 반려", "요청하신 연차 이동이 관리자에 의해 반려되었어요.", param.changeReqId(), param.gvUserCd());

        log.info("관리자 연차 변경 반려. cmpnyCd={}, changeReqId={}, by={}",
                param.gvCmpnyCd(), param.changeReqId(), param.gvUserCd());
    }

    // ============================================================
    // 근로자(앱)
    // ============================================================

    @Override
    public List<LeaveChangeRequestRowResult> getPendingConsents(String cmpnyCd, String userCd) {
        // 본인 대상 REQUESTED 요청만(스코프=본인 userCd, 상태=REQUESTED, 사업장 무관)
        return attd13Mapper.selectPendingConsents(cmpnyCd, userCd);
    }

    @Override
    @Transactional
    public void respondChangeRequest(String cmpnyCd, String userCd, String changeReqId,
                                     String workerResponse, String responseReason) {
        // 1) 요청 재조회 + 본인 대상 확인(IDOR — body 비신뢰, JWT userCd 기준)
        LeaveChangeRequestRowResult req = attd13Mapper.selectChangeRequest(cmpnyCd, changeReqId);
        if (req == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_121);
        }
        if (!userCd.equals(req.targetUserCd())) {
            throw new ApiException(AttdErrorCode.ATTD_403_121);
        }
        // 2) REQUESTED 만 응답 가능(멱등 — 중복 응답 방지)
        if (!STATUS_REQUESTED.equals(req.reqStatus())) {
            throw new ApiException(AttdErrorCode.ATTD_409_120);
        }

        String newStatus = RESPONSE_AGREE.equals(workerResponse) ? STATUS_AGREED : STATUS_REJECTED;
        // 3) 상태 전이(REQUESTED → AGREED/REJECTED). WHERE 못박음으로 동시성 방어.
        int updated = attd13Mapper.applyWorkerResponse(
                cmpnyCd, changeReqId, userCd, workerResponse, responseReason, newStatus, userCd);
        if (updated == 0) {
            throw new ApiException(AttdErrorCode.ATTD_409_120);
        }

        // 4) 관리자(발의자) PUSH 적재(응답 결과). 거부 시 원 연차 불변(상태만 REJECTED).
        String body = RESPONSE_AGREE.equals(workerResponse)
                ? "근로자가 연차 변경에 동의했어요. 최종 확인을 진행해 주세요."
                : "근로자가 연차 변경을 거부했어요.";
        enqueuePush(cmpnyCd, req.siteCd(), req.initiatorUserCd(), NOTI_RESPONSE, "연차 변경 응답", body, changeReqId, userCd);

        log.info("근로자 연차 변경 응답. cmpnyCd={}, changeReqId={}, response={}, by={}",
                cmpnyCd, changeReqId, workerResponse, userCd);
    }

    @Override
    public List<MovableLeaveResult> getMovableLeaves(String cmpnyCd, String userCd) {
        return attd13Mapper.selectMovableLeaves(cmpnyCd, userCd, todayYmd());
    }

    @Override
    @Transactional
    public void createWorkerMoveRequest(String cmpnyCd, String userCd, String targetLeaveId,
                                        String moveTargetDate, String reqReason) {
        // 1) 대상 연차 재조회(본인 소유 확인 — IDOR fail-closed)
        LeaveUseTargetResult target = loadConfirmedTarget(cmpnyCd, targetLeaveId);
        if (!userCd.equals(target.userCd())) {
            throw new ApiException(AttdErrorCode.ATTD_404_120);
        }
        // 1-1) 미래일 가드(D2): picker(selectMovableLeaves)와 동일하게 출발일이 오늘 이후인 연차만 이동 발의 가능.
        //      과거 확정 연차 직접 POST 우회 차단.
        if (target.startDate() == null || target.startDate().compareTo(todayYmd()) < 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_129);
        }
        // 2) 마감 가드(출발일 + 이동 대상일)
        ensureNotClosed(cmpnyCd, target.siteCd(), target.userCd(), target.startDate());
        ensureNotClosed(cmpnyCd, target.siteCd(), target.userCd(), moveTargetDate);
        // 3) 이동 검증(만료 이내 + DIRECT_USE_KEY 충돌)
        validateMove(cmpnyCd, target, moveTargetDate);

        // 4) REQUESTED 생성(WORKER 발의, MOVE 전용)
        String changeReqId = insertRequest(cmpnyCd, target.siteCd(), target.userCd(), target.leaveId(),
                INITIATOR_WORKER, REQ_TYPE_MOVE, moveTargetDate, reqReason, userCd);

        // 5) 관리자 승인은 attd13 confirm 흐름과 통합(WORKER 발의도 AGREED 후 확인 시 반영).
        //    근로자 발의는 별도 응답 단계가 불요하므로 생성 즉시 AGREED 로 둔다(관리자 승인=START_DATE 갱신 / 반려=불변).
        //    (D5) 반환 0행이면 직전 INSERT 와의 불일치(고아 REQUESTED 행) — 충돌로 처리하여 롤백.
        int agreed = attd13Mapper.applyWorkerResponse(
                cmpnyCd, changeReqId, userCd, RESPONSE_AGREE, null, STATUS_AGREED, userCd);
        if (agreed == 0) {
            throw new ApiException(AttdErrorCode.ATTD_409_120);
        }

        log.info("근로자 연차 이동 발의(AGREED). cmpnyCd={}, changeReqId={}, leaveId={}, moveTo={}, by={}",
                cmpnyCd, changeReqId, target.leaveId(), moveTargetDate, userCd);
    }

    // ============================================================
    // 내부 헬퍼
    // ============================================================

    /** 확정(CONFIRMED)·미삭제 대상 연차를 재조회한다. 없으면 404. */
    private LeaveUseTargetResult loadConfirmedTarget(String cmpnyCd, String leaveId) {
        LeaveUseTargetResult target = attd13Mapper.selectLeaveUseTarget(cmpnyCd, leaveId);
        if (target == null || !LEAVE_STATUS_CONFIRMED.equals(target.leaveStatus())) {
            throw new ApiException(AttdErrorCode.ATTD_404_120);
        }
        return target;
    }

    /**
     * REQUESTED 1건 INSERT(활성요청 멱등). UNIQUE(ACTIVE_LEAVE_KEY) 위반 시 진행중 요청 충돌로 변환.
     *
     * @return 채번된 changeReqId
     */
    private String insertRequest(String cmpnyCd, String siteCd, String userCd, String leaveId,
                                 String initiatorType, String reqType, String moveTargetDate,
                                 String reqReason, String initiatorUserCd) {
        String changeReqId = attd13Mapper.selectNextChangeReqId(cmpnyCd);
        LeaveChangeRequestInsertCommand cmd = new LeaveChangeRequestInsertCommand(
                changeReqId, cmpnyCd, siteCd, userCd, leaveId, initiatorType, reqType,
                moveTargetDate, reqReason, initiatorUserCd, initiatorUserCd);
        try {
            attd13Mapper.insertChangeRequest(cmd);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 동일 LEAVE_ID 에 이미 활성(REQUESTED/AGREED) 요청 존재(§7 멱등)
            throw new ApiException(AttdErrorCode.ATTD_400_128);
        }
        return changeReqId;
    }

    /** 이동 검증: 만료일(AVAIL_TO_DATE) 이내 + 이동 대상일 DIRECT_USE_KEY 충돌(동일 직원·연차코드·일자) 거부. */
    private void validateMove(String cmpnyCd, LeaveUseTargetResult target, String moveTargetDate) {
        // 만료 초과(§3-2): grant 만료일이 있으면 이동 대상일이 그 이내여야 함
        if (target.grantId() != null && !target.grantId().isBlank()) {
            String availTo = attd13Mapper.selectGrantAvailToDate(cmpnyCd, target.grantId());
            if (availTo != null && moveTargetDate.compareTo(availTo) > 0) {
                throw new ApiException(AttdErrorCode.ATTD_400_125);
            }
        }
        // DIRECT_USE_KEY 충돌(§2-2): 이동 대상일에 동일 연차 기존재
        int conflict = attd13Mapper.countLeaveUseOnDate(
                cmpnyCd, target.userCd(), target.leaveCd(), moveTargetDate, target.leaveId());
        if (conflict > 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_126);
        }
    }

    /** MOVE 반영: START_DATE 갱신(ORIG_DESIGNATED_DATE 보존). */
    private void applyMove(String cmpnyCd, LeaveUseTargetResult target, String newDate, String operatorUserCd) {
        attd13Mapper.moveLeaveUseDate(cmpnyCd, target.leaveId(), newDate, operatorUserCd);
        // prafta-com-008-E-2: 출근 차단은 leave_use(START_DATE) 기준 → moveLeaveUseDate 만으로 차단일 이동.
        //   work_plan 은 SCH_CD 유지(연차블록 미조정). 이동은 차감량 불변 → GRANT 재계산 불요.
    }

    /** DELETE 반영: 사용행 soft cancel + 부여 USED_DAYS 재계산(차감 복원). */
    private void applyDelete(String cmpnyCd, LeaveUseTargetResult target, String operatorUserCd) {
        attd13Mapper.cancelLeaveUse(cmpnyCd, target.leaveId(), CANCEL_REASON_CHANGE_DELETE, operatorUserCd);
        // 차감 복원: 연결된 부여의 USED_DAYS 를 잔존 CONFIRMED 합계로 재계산(소멸임박 grant 영향은 자연 반영)
        if (target.grantId() != null && !target.grantId().isBlank()) {
            leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, target.grantId(), operatorUserCd);
        }
        // LC-05(F1): 삭제 대상이 시간차(02/03/04)면 그날 잔존 시간차 건을 시간순 재적용해
        //   하한 차액 배치를 보정한다(잔존 건 LEAVE_DAYS 재산출 + 영향 GRANT 재집계).
        if (isHourlyUnit(target.useUnitType())) {
            leaveHourlyResettleService.resettleHourlyLeaveOnDate(
                    cmpnyCd, target.siteCd(), target.userCd(), target.startDate(), operatorUserCd);
        }
        // prafta-com-008-E-2: 출근 차단은 leave_use 기준 → cancelLeaveUse 로 자동 해제(work_plan SCH_CD 유지).
    }

    /** 시간차(SYS025 02/03/04) 단위 여부 — LC-05 재정산 훅 대상 판정. */
    private boolean isHourlyUnit(String useUnitType) {
        return "02".equals(useUnitType) || "03".equals(useUnitType) || "04".equals(useUnitType);
    }

    /**
     * PUSH outbox 1건 적재(SEND_STATUS='PENDING'). DATA_PAYLOAD 에는 비-PII 식별자(changeReqId, type)만 담는다(작업3).
     * 앱이 페이로드의 changeReqId 로 해당 요청을 곧장 열 수 있게 한다(딥링크). PII 평문(이름/휴대폰)은 금지. 예외 격리.
     */
    private void enqueuePush(String cmpnyCd, String siteCd, String targetUserCd,
                            String notiType, String title, String body,
                            String changeReqId, String operatorUserCd) {
        try {
            if (targetUserCd == null || targetUserCd.isBlank()) {
                return; // 발의자 미상 등 — 미발송
            }
            NotiOutboxInsertVO vo = new NotiOutboxInsertVO();
            vo.setNotiId(leaveDashboardMapper.selectNextNotiId(cmpnyCd));
            vo.setCmpnyCd(cmpnyCd);
            vo.setSiteCd(siteCd);
            vo.setTargetUserCd(targetUserCd);
            vo.setNotiType(notiType);
            vo.setChannel(CHANNEL_PUSH);
            vo.setTitle(title);
            vo.setBody(body);
            vo.setDataPayload(buildDeeplinkPayload(notiType, changeReqId)); // 비-PII 식별자만(딥링크)
            vo.setSendStatus(SEND_STATUS_PENDING);
            vo.setDedupKey(null); // 동일 LEAVE_ID 다회 이벤트 허용(요청/응답/확인/반려) → dedup 미사용
            vo.setInsertNo(operatorUserCd);
            leaveDashboardMapper.insertNotiOutbox(vo);
        } catch (Exception e) {
            log.error("연차 변경 PUSH 적재 실패(본 흐름 영향 없음). cmpnyCd={}, notiType={}, target={}",
                    cmpnyCd, notiType, targetUserCd, e);
        }
    }

    /**
     * PUSH 딥링크 DATA_PAYLOAD(JSON). 비-PII 식별자만(type, changeReqId) — 앱이 해당 요청을 열 수 있게 한다(작업3).
     * leaverefusal buildAlertPayload 패턴 미러. 직렬화 실패 시 빈 객체 폴백.
     */
    private String buildDeeplinkPayload(String notiType, String changeReqId) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", notiType);
        if (changeReqId != null && !changeReqId.isBlank()) {
            data.put("changeReqId", changeReqId);
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("연차 변경 PUSH payload 직렬화 실패. notiType={}, changeReqId={}", notiType, changeReqId, e);
            return "{}";
        }
    }

    /**
     * 노드(부서) 스코프 관리 권한 검증 — master/hr(전사) 또는 해당/상위 부서 정·부 관리자.
     * (작업1) safe 제외: canManageNodeExcludeSafe 사용.
     */
    private void ensureCanManageScope(String authCd, String userCd, String cmpnyCd, String siteCd, String nodeCd) {
        if (!attdCloseService.canManageNodeExcludeSafe(authCd, userCd, cmpnyCd, siteCd, nodeCd)) {
            log.warn("연차 변경 스코프 권한 없음 - userCd={}, authCd={}, nodeCd={}", userCd, authCd, nodeCd);
            throw new ApiException(AttdErrorCode.ATTD_403_120);
        }
    }

    /**
     * 대상 사용자 관리 권한 검증 — master/hr(전사) 또는 대상자 소속/상위 부서 정·부 관리자(클라이언트 nodeCd 불신뢰).
     * (작업1) safe 제외: canManageUserExcludeSafe 사용.
     */
    private void ensureCanManageUser(String authCd, String requesterUserCd, String cmpnyCd, String siteCd, String targetUserCd) {
        if (!attdCloseService.canManageUserExcludeSafe(authCd, requesterUserCd, cmpnyCd, siteCd, targetUserCd)) {
            log.warn("연차 변경 대상자 권한 없음 - requester={}, authCd={}, target={}", requesterUserCd, authCd, targetUserCd);
            throw new ApiException(AttdErrorCode.ATTD_403_120);
        }
    }

    /** 마감 가드(§3-2-1): 사용자 소속부서 기준 해당 월(YYYYMM)이 마감 커버리지면 거부. */
    private void ensureNotClosed(String cmpnyCd, String siteCd, String userCd, String ymd) {
        if (ymd == null || ymd.length() < 6) {
            return;
        }
        String closeYm = ymd.substring(0, 6);
        if (attdCloseService.isClosedForUser(cmpnyCd, siteCd, userCd, closeYm)) {
            throw new ApiException(AttdErrorCode.ATTD_400_127);
        }
    }

    private String todayYmd() {
        LocalDate d = LocalDate.now();
        return String.format("%04d%02d%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }
}
