package com.prafta.web.attd.attd13.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.service.LeaveDeductionService;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;
import com.prafta.common.cmm.leave.service.LeaveHourlyResettleService;
import com.prafta.common.cmm.leave.service.LeaveRemnantCoverService;
import com.prafta.common.cmm.leave.util.HourlyLeaveChargeUtils;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.HalfDayBoundary;
import com.prafta.common.cmm.leave.vo.HourlyChargeVO;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.cmm.leave.vo.RemnantTriggerPlanVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AdvisoryLockTxUtils;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.DateTimeUtils;
import com.prafta.web.attd.attd13.application.command.LeaveChangeRequestInsertCommand;
import com.prafta.web.attd.attd13.application.command.MovedLeaveUseInsertCommand;
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
import com.prafta.web.attd.leaveflow.service.impl.LeaveFlowServiceImpl;
import com.prafta.web.attd.leaveflow.service.impl.LeaveFlowServiceImpl.GrantCharge;

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
    /** 연차사용 요청(TB_USER_ATTD_REQ REQ_TYPE='05'/'06')이 결재 대기 중인 상태[SYS033 01:신청]. */
    private static final String ATTD_REQ_STATUS_REQUESTED = "01";
    private static final String UNIT_FULL = "00"; // 일 단위(출근 차단 블록 대상)
    private static final String UNIT_HALF = "01"; // 반차(T1 재차감 고정 요금 0.5)
    private static final String UNIT_QUARTER = "05"; // 반반차(T1 재차감 고정 요금 0.25)
    private static final String WHOLE_SITE = "*"; // 전체 부서 스코프(노드 관리자는 사용 불가)

    /** T1 이동 반영 advisory lock 타임아웃(초) — 신청 경로 관례 미러. 타임아웃 시 ATTD_409_071(§2-1). */
    private static final int MOVE_LOCK_TIMEOUT_SEC = 5;

    private static final String CHANNEL_PUSH = "PUSH";
    private static final String SEND_STATUS_PENDING = "PENDING";
    private static final String NOTI_REQUEST = "LEAVE_CHANGE_REQUEST";
    private static final String NOTI_RESPONSE = "LEAVE_CHANGE_RESPONSE";
    private static final String NOTI_CONFIRMED = "LEAVE_CHANGE_CONFIRMED";
    private static final String NOTI_REJECTED = "LEAVE_CHANGE_REJECTED";

    private static final String CANCEL_REASON_CHANGE_DELETE = "관리자 연차 삭제(동의 확정)";
    /** T1: 이동 확정 시 원 차감 취소 사유(고정 문구 — §2-2 3단계). */
    private static final String CANCEL_REASON_CHANGE_MOVE = "연차 이동(동의 확정) — 재차감";

    private final Attd13Mapper attd13Mapper;
    private final AttdCloseService attdCloseService;
    /** 사업장 접근 인가(공용 cmm 빈) — 토큰 사업장 등식 대신 User_03 원장(TB_USER_SITE_AUTH) 기반 인가. */
    private final com.prafta.common.cmm.siteauth.service.SiteAccessService siteAccessService;
    private final LeaveFlowMapper leaveFlowMapper;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;
    /** LC-05(F1): 시간차 행 삭제 시 그날 잔존 시간차 건 시간순 재정산(코어 산식 LC-03 공유). */
    private final LeaveHourlyResettleService leaveHourlyResettleService;
    /** PC-06(D7): 삭제(동의 확정)로 잔여 복원 시 미도래 짜투리 보전 건 회수(정상 차감 전환). */
    private final LeaveRemnantCoverService leaveRemnantCoverService;
    /** T1: 이동 재차감 요금 재산출(시간차 calcHourlyCharge — 신청 산식 단일 출처 재사용). */
    private final LeaveDeductionService leaveDeductionService;
    /** T1: 짜투리 재판정 입력(personalConvMinutes) — PC-03 단일 출처 재사용. */
    private final LeaveConversionPolicyService leaveConversionPolicyService;
    /** T1: PC-02 만료임박순 다부여 할당(resolveGeneralCharges) 재사용 — 신청 경로와 단일 출처(가시성만 공개화). */
    private final LeaveFlowServiceImpl leaveFlowServiceImpl;
    /** F9(qa D-4): 삭제 확정 시 가불 GRANT 회수 — 05 반려(restoreLeaveLedger)의 cancelBorrowGrantByReqId 재사용. */
    private final LeaveGrantEngineService leaveGrantEngineService;

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
            // 사업장 접근 인가(구 토큰 사업장 등식 가드 대체 — User_03 원장 기반).
            siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());
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
            // 이동 검증(형식·과거일·동일일 F3·만료 F1·충돌) + 대상일 잔여 soft 체크(§2-6)
            validateMove(param.gvCmpnyCd(), target, param.moveTargetDate());
            validateMoveBalanceSoft(param.gvCmpnyCd(), target, param.moveTargetDate());
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
        //    body 는 reqType 분기(2026-08-18 B-2 승인 — 취소 건 "이동" 오문구 방지, 문구 외 불변).
        String rejectBody = REQ_TYPE_MOVE.equals(req.reqType())
                ? "요청하신 연차 이동이 관리자에 의해 반려되었어요."
                : "요청하신 연차 취소가 관리자에 의해 반려되었어요.";
        enqueuePush(param.gvCmpnyCd(), req.siteCd(), req.initiatorUserCd(), NOTI_REJECTED,
                "연차 변경 반려", rejectBody, param.changeReqId(), param.gvUserCd());

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
        // 3) 이동 검증(형식·과거일·동일일 F3·만료 F1·충돌) + 대상일 잔여 soft 체크(§2-6)
        validateMove(cmpnyCd, target, moveTargetDate);
        validateMoveBalanceSoft(cmpnyCd, target, moveTargetDate);

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

    @Override
    @Transactional
    public void createWorkerDeleteRequest(String cmpnyCd, String userCd, String targetLeaveId, String reqReason) {
        // 1) 대상 연차 재조회(본인 소유 확인 — IDOR fail-closed)
        LeaveUseTargetResult target = loadConfirmedTarget(cmpnyCd, targetLeaveId);
        if (!userCd.equals(target.userCd())) {
            throw new ApiException(AttdErrorCode.ATTD_404_120);
        }
        // 1-1) 미래일 가드(이동 D2 미러): 출발일이 오늘 이후인 연차만 취소 발의 가능.
        //      과거 확정 연차 직접 POST 우회 차단(과거 연차일 취소는 근태 보정/관리자 영역 유지).
        if (target.startDate() == null || target.startDate().compareTo(todayYmd()) < 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_206);
        }
        // 1-2) 촉진 가드(2026-08-18 사용자 확정 — 보수): 촉진 지정(FIRST/SECOND) 건은 근로자 취소 발의 차단.
        //      임의 취소가 노무수령거부 판정 연속성을 끊는 것을 원천 차단(이동 F2 판별식 재사용).
        //      관리자 발의 삭제(createChangeRequest)는 현행대로 촉진 가드 없음 — 근로자 발의만 차단.
        if (target.promotionStage() != null && !"NONE".equals(target.promotionStage())) {
            log.info("근로자 연차 취소 발의 거부: 촉진 지정 건. cmpnyCd={}, leaveId={}, stage={}",
                    cmpnyCd, target.leaveId(), target.promotionStage());
            throw new ApiException(AttdErrorCode.ATTD_400_207);
        }
        // 2) 마감 가드(출발일만 — DELETE 는 이동 대상일 없음)
        ensureNotClosed(cmpnyCd, target.siteCd(), target.userCd(), target.startDate());
        // (validateMove / validateMoveBalanceSoft 미호출 — 관리자 발의 DELETE 경로와 동일)

        // 3) REQUESTED 생성(WORKER 발의, DELETE 전용 — moveTargetDate 없음)
        String changeReqId = insertRequest(cmpnyCd, target.siteCd(), target.userCd(), target.leaveId(),
                INITIATOR_WORKER, REQ_TYPE_DELETE, null, reqReason, userCd);

        // 4) 근로자 발의는 별도 응답 단계가 불요하므로 생성 즉시 AGREED 로 둔다(이동 발의 관례 미러 —
        //    관리자 승인=applyDelete 반영 / 반려=원 연차 불변).
        //    (D5) 반환 0행이면 직전 INSERT 와의 불일치(고아 REQUESTED 행) — 충돌로 처리하여 롤백.
        int agreed = attd13Mapper.applyWorkerResponse(
                cmpnyCd, changeReqId, userCd, RESPONSE_AGREE, null, STATUS_AGREED, userCd);
        if (agreed == 0) {
            throw new ApiException(AttdErrorCode.ATTD_409_120);
        }

        // 관리자 PUSH 미발송(2026-08-18 사용자 확정 B-1 — 이동 발의와 동일 수준, 로그만.
        //   관리자는 Attd_10 목록/Attd_07 카드에서 인지).
        log.info("근로자 연차 취소 발의(AGREED). cmpnyCd={}, changeReqId={}, leaveId={}, by={}",
                cmpnyCd, changeReqId, target.leaveId(), userCd);
    }

    // ============================================================
    // 내부 헬퍼
    // ============================================================

    /**
     * 확정(CONFIRMED)·미삭제 대상 연차를 재조회한다. 없으면 404.
     *
     * <p>T3 대표행 정규화: 대상이 REQ 연결 건(분할 묶음)이면 그 REQ 의 대표행(MIN LEAVE_ID)으로
     * 치환 조회한다 — 비대표행 LEAVE_ID 로 직접 POST 되어도 서버가 대표행 기준으로 저장/처리하여
     * ACTIVE_LEAVE_KEY 멱등(400_128)이 REQ 단위로 성립한다. 같은 REQ = 같은 소유자이므로 정규화
     * 후의 권한/소유자 검증(호출부)은 원 입력과 동일 대상을 검증한다(IDOR 우회 없음).
     */
    private LeaveUseTargetResult loadConfirmedTarget(String cmpnyCd, String leaveId) {
        LeaveUseTargetResult target = attd13Mapper.selectLeaveUseTarget(cmpnyCd, leaveId);
        if (target == null || !LEAVE_STATUS_CONFIRMED.equals(target.leaveStatus())) {
            throw new ApiException(AttdErrorCode.ATTD_404_120);
        }
        if (target.reqId() != null && !target.reqId().isBlank()) {
            String repId = attd13Mapper.selectRepresentativeLeaveId(cmpnyCd, target.reqId());
            if (repId != null && !repId.equals(target.leaveId())) {
                target = attd13Mapper.selectLeaveUseTarget(cmpnyCd, repId);
                if (target == null || !LEAVE_STATUS_CONFIRMED.equals(target.leaveStatus())) {
                    throw new ApiException(AttdErrorCode.ATTD_404_120);
                }
            }
        }
        // 2026-08-14: 결재 진행 중('01')인 연차사용 요청에 연결된 건은 변경/삭제 대상이 아니다(fail-closed).
        //   연차 신청 시점에 TB_USER_LEAVE_USE 가 CONFIRMED 로 선차감 생성되므로, 종전에는 결재가
        //   끝나지 않은 연차도 leaveStatus 만 보고 대상이 되었다. 그 결과 삭제 확정 → 이후 결재 승인
        //   순서로 진행되면 "요청은 승인(02)인데 사용실적은 CANCELLED" 인 모순 상태가 남았다(운영 확인:
        //   REQ 2026081400214 / LV2026081400130). 결재 중인 건의 정상 경로는 신청 취소 또는 결재 반려다.
        //   ★본 헬퍼는 관리자 발의(createChangeRequest)·관리자 확인(confirmChangeRequest)·
        //     근로자 이동 발의(createWorkerMoveRequest) 세 진입점이 모두 거치므로 여기 한 곳으로 전부 막힌다.
        //   결재 미경유(직접 차감) 건은 reqStatus 가 null 이라 통과한다(동작 불변).
        if (ATTD_REQ_STATUS_REQUESTED.equals(target.reqStatus())) {
            log.info("연차 변경/삭제 거부: 결재 진행 중인 연차. cmpnyCd={}, leaveId={}, reqId={}",
                    cmpnyCd, target.leaveId(), target.reqId());
            throw new ApiException(AttdErrorCode.ATTD_400_135);
        }
        return target;
    }

    /**
     * T1/T3: 재차감·충돌 검증 기준 연차 종류 — REQ 연결 건은 REQ 원 종류(TB_USER_ATTD_REQ.LEAVE_TYPE,
     * PC-05 발동 건은 행 LEAVE_CD 가 부여 귀속이라 다를 수 있음), 직접사용은 행 종류.
     */
    private String resolveEffectiveLeaveCd(LeaveUseTargetResult target) {
        if (target.reqId() != null && !target.reqId().isBlank()
                && target.reqLeaveCd() != null && !target.reqLeaveCd().isBlank()) {
            return target.reqLeaveCd();
        }
        return target.leaveCd();
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

    /**
     * 이동 검증(발의·확인 재검증 공통): 형식·실재 날짜 + 과거일 + 동일일 + 만료일 이내 + DIRECT_USE_KEY 충돌.
     *
     * <ul>
     *   <li>F7c(sec Low-003): YYYYMMDD 형식 + LocalDate 실재 검증(비실재 날짜 "20261399" 등 거부) —
     *       DTO @Pattern 1차 방어에 더해 확정 경로의 DB 저장값 재검증까지 커버.</li>
     *   <li>F3(qa D-2): 출발일 == 대상일 이동 거부(동일일 재차감의 시간차 요금 왜곡·부여 상한 우회 봉합).</li>
     *   <li>F1(2026-08-04 사용자 확정 — 만료 초과 허용 철회): 이동 대상일이 원 차감 부여들의
     *       min(AVAIL_TO_DATE) 초과면 ATTD_400_125 거부. "이동"이 올해분 소멸+내년분 차감을 숨기는
     *       함정 방지 — 만료 이후 날짜가 필요하면 삭제 후 재신청이 정답 흐름(com-008-C §3-2 원 조항 복원).</li>
     *   <li>충돌 검증은 T3 확장 — 자기 REQ 전 행(분할 묶음) 제외 + 종류 = REQ 원 종류.</li>
     * </ul>
     */
    private void validateMove(String cmpnyCd, LeaveUseTargetResult target, String moveTargetDate) {
        // F7c: 형식(YYYYMMDD) + 실재 날짜 검증. BASIC_ISO_DATE 는 STRICT 리졸버라 2월 31일 등도 거부.
        if (moveTargetDate == null || !moveTargetDate.matches("\\d{8}")) {
            throw new ApiException(AttdErrorCode.ATTD_400_131);
        }
        try {
            LocalDate.parse(moveTargetDate, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            log.info("이동 대상일 비실재 날짜 거부. cmpnyCd={}, leaveId={}, moveTargetDate={}",
                    cmpnyCd, target.leaveId(), moveTargetDate);
            throw new ApiException(AttdErrorCode.ATTD_400_131);
        }
        // 이동 대상일 과거 날짜 가드(오늘 포함 허용, 그 이전만 차단) — 관리자/근로자 발의·확인 재검증 3경로 공통.
        if (moveTargetDate.compareTo(todayYmd()) < 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_131);
        }
        // F3: 동일일 이동 거부(출발일 == 대상일).
        if (moveTargetDate.equals(target.startDate())) {
            throw new ApiException(AttdErrorCode.ATTD_400_134);
        }
        // F1: 만료일 이내 제한 — 원 차감 부여들의 최소 만료일(min AVAIL_TO_DATE) 초과 거부.
        //   REQ 묶음은 묶음 전 행 기준, 직접사용은 원 GRANT 단건(F4 단건 재차감과 정합). 만료일 정보가
        //   없으면(grant 미연결 등 이례 데이터) 판정 불가 — 확정 재차감의 잔여 검증에 위임(스킵).
        String minAvailTo;
        if (target.reqId() != null && !target.reqId().isBlank()) {
            minAvailTo = attd13Mapper.selectMinGrantAvailToDateByReq(cmpnyCd, target.reqId());
        } else if (target.grantId() != null && !target.grantId().isBlank()) {
            minAvailTo = attd13Mapper.selectGrantAvailToDate(cmpnyCd, target.grantId());
        } else {
            minAvailTo = null;
        }
        if (minAvailTo != null && !minAvailTo.isBlank() && moveTargetDate.compareTo(minAvailTo) > 0) {
            log.info("이동 대상일 만료 초과 거부(F1). cmpnyCd={}, leaveId={}, moveTo={}, minAvailTo={}",
                    cmpnyCd, target.leaveId(), moveTargetDate, minAvailTo);
            throw new ApiException(AttdErrorCode.ATTD_400_125);
        }
        // DIRECT_USE_KEY 충돌(§2-2): 이동 대상일에 동일 연차 기존재(자기 자신 + 자기 REQ 묶음 제외)
        int conflict = attd13Mapper.countLeaveUseOnDate(
                cmpnyCd, target.userCd(), resolveEffectiveLeaveCd(target), moveTargetDate,
                target.leaveId(), target.reqId());
        if (conflict > 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_126);
        }
        // 2026-08-17(A안 후속): 시간차(02/03/04)는 시각 구간을 원값 그대로 승계하므로 대상일에서도
        //   "근무시간 내" + "휴게 가로지름 금지"를 신청 경로와 동일하게 검증한다. 종전엔 미검증이라
        //   근무시간 밖에 떠 있는 시간차(쉬는 효과 없이 차감만 발생)가 만들어질 수 있었다.
        //   반차('01')는 applyMove 가 대상일 스케줄로 경계를 재산출하므로 대상이 아니고,
        //   시각 결손 구 데이터는 판정 불가 — 스킵(추정 금지, 종전 동작 유지).
        //   발의(관리자/근로자)·확정 재검증 3경로가 본 메서드를 공유하므로 여기 한 곳이 단일 지점이다.
        if (isHourlyUnit(target.useUnitType())) {
            Integer sMin = DateTimeUtils.hhmmToMinutes(target.startTime());
            Integer eMin = DateTimeUtils.hhmmToMinutes(target.endTime());
            if (sMin != null && eMin != null && eMin > sMin) {
                if (!leaveDeductionService.withinScheduledWorkHours(
                        cmpnyCd, target.siteCd(), target.userCd(), moveTargetDate, sMin, eMin)) {
                    log.info("연차 이동 거부: 대상일 근무시간 밖 시간차. cmpnyCd={}, leaveId={}, moveTo={}, {}~{}",
                            cmpnyCd, target.leaveId(), moveTargetDate, target.startTime(), target.endTime());
                    throw new ApiException(AttdErrorCode.ATTD_400_103);
                }
                if (leaveDeductionService.crossesBreak(
                        cmpnyCd, target.siteCd(), target.userCd(), moveTargetDate, sMin, eMin)) {
                    log.info("연차 이동 거부: 대상일 휴게 가로지름. cmpnyCd={}, leaveId={}, moveTo={}, {}~{}",
                            cmpnyCd, target.leaveId(), moveTargetDate, target.startTime(), target.endTime());
                    throw new ApiException(AttdErrorCode.ATTD_400_055);
                }
            }
        }
    }

    /**
     * T1 발의 시 사전 soft 체크(§2-6 — 안내 목적, FOR UPDATE 미사용): 대상일 기준
     * "합산 잔여 + 자기 묶음 복원 예상분(취소 시 되살아나는 대상일 유효·원 종류 귀속 차감분)" 이
     * 요금에 못 미치면 무의미한 동의 사이클을 막는다.
     *
     * <ul>
     *   <li>직접사용(REQ_ID NULL): PC-05 재판정 비대상 — 확정 시 확실히 실패하므로 즉시 ATTD_400_132 거부.</li>
     *   <li>REQ 연결 건: 확정 시 취소 후 잔여 기준 PC-05 재판정(§2-2 5-b)이 성립할 수 있어 발의 시점엔
     *       판정 불가 — 거부하지 않고 통과(확정 재검증이 단일 신뢰 지점, §2-6). 로그만 남긴다.</li>
     * </ul>
     */
    private void validateMoveBalanceSoft(String cmpnyCd, LeaveUseTargetResult target, String moveTargetDate) {
        String leaveCd = resolveEffectiveLeaveCd(target);
        String unit = target.useUnitType();
        BigDecimal fee;
        if (UNIT_FULL.equals(unit)) {
            fee = new BigDecimal("1.00000");
        } else if (UNIT_HALF.equals(unit)) {
            fee = new BigDecimal("0.50000");
        } else if (UNIT_QUARTER.equals(unit)) {
            fee = new BigDecimal("0.25000");
        } else if (isHourlyUnit(unit)) {
            Integer minutes = target.leaveMinutes();
            if (minutes == null || minutes <= 0) {
                // 대표행 분 결손(불변식 위반 데이터 방어) — 발의 단계 판정 불가, 확정 재검증에 위임.
                log.warn("이동 발의 soft 체크 생략(대표행 분 결손). cmpnyCd={}, leaveId={}", cmpnyCd, target.leaveId());
                return;
            }
            // 대상일 스케줄 없음/분모 산출 불가 등은 calcHourlyCharge 의 해당 에러 그대로 전파(§2-2 주석).
            HourlyChargeVO hc = leaveDeductionService.calcHourlyCharge(
                    cmpnyCd, target.siteCd(), target.userCd(), moveTargetDate, minutes);
            if (hc == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            fee = hc.chargeDays();
        } else {
            return; // 알 수 없는 단위 — 확정 재검증에 위임(보수: 발의 차단 안 함)
        }
        if (fee.signum() <= 0) {
            return;
        }
        BigDecimal remaining = nz(leaveFlowMapper.selectDeductibleRemainingSum(
                cmpnyCd, target.userCd(), leaveCd, moveTargetDate));
        BigDecimal selfRestorable = nz(attd13Mapper.sumSelfRestorableDaysOnDate(
                cmpnyCd, target.reqId(), target.leaveId(), leaveCd, moveTargetDate));
        if (remaining.add(selfRestorable).compareTo(fee) >= 0) {
            return;
        }
        if (target.reqId() != null && !target.reqId().isBlank()) {
            log.info("이동 발의 soft 체크: 잔여 부족이나 PC-05 재판정 여지로 발의 허용(확정이 최종 판단). "
                            + "cmpnyCd={}, reqId={}, fee={}, remaining={}, selfRestorable={}",
                    cmpnyCd, target.reqId(), fee.toPlainString(), remaining.toPlainString(),
                    selfRestorable.toPlainString());
            return;
        }
        log.info("이동 발의 거부(대상일 잔여 부족 — 직접사용). cmpnyCd={}, leaveId={}, fee={}, remaining={}, 복원예상={}",
                cmpnyCd, target.leaveId(), fee.toPlainString(), remaining.toPlainString(),
                selfRestorable.toPlainString());
        throw new ApiException(AttdErrorCode.ATTD_400_132);
    }

    /**
     * T1: MOVE 반영 — "원 차감 취소 + 대상일 재차감" (plan §2-2, 구 START_DATE UPDATE 방식 폐기).
     *
     * <p>비분할(1행)·직접사용(REQ_ID NULL) 건도 동일 경로(fast path 이원화 금지 — 사용자 확정).
     * 확정 트랜잭션(confirmChangeRequest) 안에서 호출되며, 실패(ATTD_400_132 등) 시 상태 전이까지
     * 전체 롤백되어 요청은 AGREED 잔류한다(§2-1).
     *
     * <p>잠금(§2-1): 신청 경로 관례 미러 — leaveDay lock → remnant lock 순서 고정(역전 시 GET_LOCK
     * 교착 실사례 M-1). 시간차는 원일자·대상일 두 leaveDay 키를 사전순으로 획득(반대 방향 동시 이동
     * 간 순서 고정), 고정단위는 신청 경로와 동일하게 day lock 미사용. 재정산/회수 훅의 동일 키
     * 재획득은 MySQL GET_LOCK 재진입(획득 횟수만큼 해제) + afterCompletion 등록이 획득별로 반복되어
     * 균형이 맞는다. 해제는 트랜잭션 완료(afterCompletion) 시점(등록 실패 시 finally 폴백).
     * F7a(sec Low-001): REQ 연결 건의 remnant lock 은 취소/재계산(부여 행 X-lock) <b>전에</b> 선획득한다
     * (5-b 재판정·7단계 reclaim 은 재진입) — 부여 행을 잡은 채 remnant 를 기다리는 역전 봉합.
     *
     * <p>수정 배치 F1~F7(2026-08-04 사용자 확정): 만료 초과 이동 허용은 철회(F1 — validateMove 에서
     * ATTD_400_125 거부), 동일일 이동 거부(F3), 직접사용은 원 GRANT 유지 단건 재차감(F4), 하루 점유·
     * 시간대 겹침 미러 가드(F5), EVIDENCE_FILE_ID 승계(F6), 촉진 건 재발동 차단(F2).
     */
    private void applyMove(String cmpnyCd, LeaveUseTargetResult target, String newDate, String operatorUserCd) {
        final String siteCd = target.siteCd();
        final String userCd = target.userCd();
        final String unit = target.useUnitType();
        final String reqId = (target.reqId() != null && !target.reqId().isBlank()) ? target.reqId() : null;
        final String moveLeaveCd = resolveEffectiveLeaveCd(target);
        final boolean hourly = isHourlyUnit(unit);
        // 대표행 총 분(불변식 1 — 고정단위는 null 가능). 반차는 아래 D-5 재산출로 대상일 값으로 갱신된다.
        Integer totalMinutes = target.leaveMinutes();
        // 이동 후 저장할 시각(반차는 대상일 스케줄에서 재산출, 그 외는 원값 승계).
        String movedStartTime = target.startTime();
        String movedEndTime = target.endTime();

        // key → afterCompletion 등록 여부(false 면 finally 에서 직접 해제)
        Map<String, Boolean> heldLocks = new LinkedHashMap<>();
        try {
            if (hourly) {
                TreeSet<String> dayKeys = new TreeSet<>(); // 사전순 고정 획득(동일 키는 자동 dedupe)
                dayKeys.add(HourlyLeaveChargeUtils.leaveDayLockKey(cmpnyCd, userCd, target.startDate()));
                dayKeys.add(HourlyLeaveChargeUtils.leaveDayLockKey(cmpnyCd, userCd, newDate));
                for (String key : dayKeys) {
                    acquireMoveLock(key, heldLocks);
                }
            }
            // F7a(sec Low-001): REQ 연결 건은 취소/재계산(3단계 — 부여 행 X-lock) 전에 remnant lock 선획득.
            //   신청 경로(N9)·회수 경로와 동일하게 "remnant → 부여 행" 순서가 되어 상호 대기(역전) 봉합.
            //   leaveDay → remnant 획득 순서 유지. 5-b 재판정·7단계 reclaim 훅의 동일 키 재획득은
            //   GET_LOCK 재진입 + 획득별 해제 등록으로 기존 균형 그대로(여기서 이중 획득하지 않는다).
            if (reqId != null) {
                acquireMoveLock(LeaveRemnantCoverService.remnantLockKey(cmpnyCd, userCd), heldLocks);
            }

            // 2) 요금 재산출(대상일 기준 — 신청 로직 미러, writer 수정 없이 조회 재사용)
            BigDecimal chargeDays;
            Integer hourlyConv = null;
            if (UNIT_FULL.equals(unit)) {
                chargeDays = new BigDecimal("1.00000");
            } else if (UNIT_HALF.equals(unit)) {
                chargeDays = new BigDecimal("0.50000");
                // ★ D-5(2026-08-07): 반차 이동은 대상일 스케줄에서 경계를 재산출한다.
                //   재산출하지 않으면 "원래 날 스케줄에서 나온 시각"이 새 날짜의 지각·조퇴·OT 판정에
                //   그대로 참여하고(미배정일로도 이동 가능) LEAVE_MINUTES 도 원래 날 값이 남는다.
                //   §8.5.10-4 "승인 시 경계 미재계산"은 E3 잠금(신청일 스케줄 변경 차단)이 전제라
                //   대상일이 바뀌는 이동에는 적용되지 않는다.
                //   시각이 없는 구 반차(START/END_TIME NULL)는 종전 동작 유지(재산출 대상 아님).
                if (movedStartTime != null && movedEndTime != null) {
                    HalfDayBoundary hbNew = leaveDeductionService.getHalfDayBoundary(
                            cmpnyCd, siteCd, userCd, newDate);
                    HalfDayBoundary hbOld = leaveDeductionService.getHalfDayBoundary(
                            cmpnyCd, siteCd, userCd, target.startDate());
                    if (hbNew == null || hbOld == null) {
                        // 대상일(또는 원일자) 스케줄 없음 → 경계 산출 불가. 신청 경로와 동일 코드로 거부.
                        log.info("연차 이동 거부(D-5): 반차 경계 산출 불가. cmpnyCd={}, leaveId={}, {}→{}, 원일자산출={}, 대상일산출={}",
                                cmpnyCd, target.leaveId(), target.startDate(), newDate,
                                hbOld != null, hbNew != null);
                        throw new ApiException(AttdErrorCode.ATTD_400_110);
                    }
                    // 파트(시작기준/종료기준)는 별도 컬럼 없이 원일자 경계에서 역산한다
                    //   (저장 시각의 시작이 그날 근무 시작과 같으면 시작기준).
                    boolean startPart = movedStartTime.equals(
                            ScheduleWorkMinutesUtils.hhmmOfDay(hbOld.workStartMin()));
                    int startMin = startPart ? hbNew.workStartMin() : hbNew.boundaryMin();
                    int endMin = startPart ? hbNew.boundaryMin() : hbNew.workEndMin();
                    movedStartTime = ScheduleWorkMinutesUtils.hhmmOfDay(startMin);
                    movedEndTime = ScheduleWorkMinutesUtils.hhmmOfDay(endMin);
                    totalMinutes = hbNew.exemptMinutes(); // 대상일 기준 면제분(= 대상일 D / 2)
                    log.info("연차 이동 반차 경계 재산출. cmpnyCd={}, leaveId={}, {}→{}, part={}, {}~{}, 면제={}분",
                            cmpnyCd, target.leaveId(), target.startDate(), newDate,
                            startPart ? "START" : "END", movedStartTime, movedEndTime, totalMinutes);
                }
            } else if (UNIT_QUARTER.equals(unit)) {
                chargeDays = new BigDecimal("0.25000");
            } else if (hourly) {
                if (totalMinutes == null || totalMinutes <= 0) {
                    // 대표행 분 결손(불변식 위반 데이터 방어) — 요금 산출 불가
                    throw new ApiException(AttdErrorCode.ATTD_400_052);
                }
                // 분모·하한·캡 재적용(E1: 분모 = 이동 대상일 당일 배정 스케줄).
                //   산출 불가(대상일 스케줄 없음 400_052 / 분모 불가 400_194)는 그대로 전파.
                HourlyChargeVO hc = leaveDeductionService.calcHourlyCharge(cmpnyCd, siteCd, userCd, newDate, totalMinutes);
                if (hc == null) {
                    throw new ApiException(AttdErrorCode.ATTD_400_052);
                }
                chargeDays = hc.chargeDays();
                hourlyConv = hc.convMinutes();
            } else {
                throw new ApiException(AttdErrorCode.ATTD_400_054);
            }

            // 3) 원 차감 취소(묶음 전 행 soft cancel) + 영향 부여 전부 잔여 복원
            List<String> grantIds;
            if (reqId != null) {
                grantIds = leaveFlowMapper.selectGrantIdsByReqId(cmpnyCd, reqId);
                leaveFlowMapper.cancelLeaveUseByReqId(cmpnyCd, reqId, CANCEL_REASON_CHANGE_MOVE, operatorUserCd);
            } else {
                grantIds = (target.grantId() != null && !target.grantId().isBlank())
                        ? List.of(target.grantId()) : List.of();
                attd13Mapper.cancelLeaveUse(cmpnyCd, target.leaveId(), CANCEL_REASON_CHANGE_MOVE, operatorUserCd);
            }
            for (String grantId : grantIds) {
                if (grantId != null && !grantId.isEmpty()) {
                    leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, grantId, operatorUserCd);
                }
            }

            // 4) 자기 cover 무효화 — 반드시 reclaimIfPossible(7단계) 전에(§0-1-2 부활 방지). T2 와 공유 메서드.
            if (reqId != null) {
                leaveRemnantCoverService.cancelCoversByReq(cmpnyCd, reqId, operatorUserCd);
            }

            // 4-b) F5(qa D-5): 신청 경로 3-B 가드 미러 — 하루 점유(400_111)·시간대 겹침(400_112).
            //   자기 REQ/자기 행은 3단계 soft cancel(같은 트랜잭션)로 CONFIRMED 술어에서 자연 제외되므로
            //   신청 경로 검증 쿼리를 무수정 재사용한다(writer 로직 불변). 판정 기준은 요금(chargeDays) —
            //   신청 경로와 동일(PC-05 발동 시 실차감은 요금보다 작아지는 방향이라 보수 판정).
            BigDecimal occupied = nz(leaveFlowMapper.selectOccupiedLeaveDaysOnDate(cmpnyCd, userCd, newDate));
            if (occupied.add(chargeDays).compareTo(BigDecimal.ONE) > 0) {
                log.info("연차 이동 거부(F5): 대상일 하루 초과 점유. cmpnyCd={}, userCd={}, moveTo={}, 점유={}, 신규={}",
                        cmpnyCd, userCd, newDate, occupied.toPlainString(), chargeDays.toPlainString());
                throw new ApiException(AttdErrorCode.ATTD_400_111);
            }
            // HB-09(D4): 겹침 검사 게이트를 "시각 보유 단위"로 확장 — 반차도 경계 시각을 갖게 되어
            //   대상일에 이미 있는 시간차/반차와의 시간대 충돌을 막는다(구 반차는 시각이 없어 자연 제외).
            //   ★ D-5: 반차는 위 2)에서 대상일 스케줄로 재산출한 시각(movedStartTime/EndTime)으로 검사한다.
            //   ★ sec N-2(2026-08-07): 판정을 SQL wrap CASE 에서 Java 로 이관(대상일 원 스케줄 프레임 정렬).
            if (movedStartTime != null && movedEndTime != null
                    && leaveDeductionService.overlapsTimeLeaveOnDate(
                            cmpnyCd, siteCd, userCd, newDate, movedStartTime, movedEndTime)) {
                log.info("연차 이동 거부(F5): 대상일 연차 시간대 겹침. cmpnyCd={}, userCd={}, moveTo={}, {}~{}",
                        cmpnyCd, userCd, newDate, movedStartTime, movedEndTime);
                throw new ApiException(AttdErrorCode.ATTD_400_112);
            }

            // 5) 재차감 할당(대상일 기준)
            String origDesignated = (target.origDesignatedDate() != null && !target.origDesignatedDate().isBlank())
                    ? target.origDesignatedDate() : target.startDate(); // 최초 지정일 보존(§2-4)
            boolean remnantTriggered = false;
            if (chargeDays.signum() > 0
                    && nz(leaveFlowMapper.selectDeductibleRemainingSum(cmpnyCd, userCd, moveLeaveCd, newDate))
                            .compareTo(chargeDays) < 0) {
                // 5-b) 잔여 부족 → REQ 연결 건이면 PC-05 재판정(원 신청과 동일 조건 입력, 대상일 기준).
                //      직접사용은 재판정 미적용(원 경로도 PC-05 비대상 — 정합) → 즉시 거부.
                if (reqId == null) {
                    throw new ApiException(AttdErrorCode.ATTD_400_132);
                }
                // F2(qa D-3): 촉진 지정(FIRST/SECOND) 건은 재발동 시 촉진 속성 승계가 유실되므로
                //   (applyTrigger 불변 — RemnantLeaveUseVO 가 승계 컬럼 미보유, 기본값 NONE/VOLUNTARY)
                //   재판정 진입 자체를 거부한다. 노무수령거부 판정 연속성 단절(촉진 수당 의무 리스크)
                //   원천 차단 — 잔여 부족 단일 사유(ATTD_400_132)로 수렴.
                if (target.promotionStage() != null && !"NONE".equals(target.promotionStage())) {
                    log.info("연차 이동 거부(F2): 촉진 지정 건은 짜투리 재발동 불가. cmpnyCd={}, reqId={}, stage={}",
                            cmpnyCd, reqId, target.promotionStage());
                    throw new ApiException(AttdErrorCode.ATTD_400_132);
                }
                // N9: 판정~기록 직렬화 — remnant lock 은 1단계에서 선획득 완료(F7a, leaveDay → remnant 순서).
                // E7: 재발동 판정의 최소 사용단위 요금도 "이동 대상일의 당일 분모" 기준 —
                //   시간차는 calcHourlyCharge 결과 재사용, 고정단위는 당일 분모 직접 조회(신청 경로 정합).
                Integer conv = (hourlyConv != null)
                        ? hourlyConv
                        : leaveConversionPolicyService.resolveDailyConvMinutes(cmpnyCd, siteCd, userCd, newDate);
                RemnantTriggerPlanVO plan = leaveRemnantCoverService.evaluateTrigger(
                        cmpnyCd, userCd, newDate, moveLeaveCd, unit, totalMinutes, chargeDays, conv);
                if (plan == null) {
                    // 재판정 미충족(T10 ⓕ 포함) — 잔여 부족으로 수렴(§2-5 파생 동작 확정)
                    throw new ApiException(AttdErrorCode.ATTD_400_132);
                }
                // 재발동: 새 use 행(잔여 전액 분할) + 새 cover(WORK_YMD=대상일) — applyTrigger 불변 재사용.
                try {
                    leaveRemnantCoverService.applyTrigger(cmpnyCd, siteCd, userCd, newDate, unit,
                            movedStartTime, movedEndTime, totalMinutes, target.leaveReason(),
                            reqId, plan, operatorUserCd);
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    // F7b(sec Low-002): 재발동 use INSERT 의 UNIQUE 경합도 일반 재차감과 동일하게
                    //   400_126 으로 변환(처리 대칭 — F2 로 도달이 희귀화되나 방어 유지).
                    throw new ApiException(AttdErrorCode.ATTD_400_126);
                }
                remnantTriggered = true;
            } else if (reqId == null) {
                // 6-a) F4(qa D-1): 직접사용(REQ_ID NULL) 이동은 다부여 분할 할당 대신 "원 GRANT_ID 유지
                //   단건 재차감"으로 처리 — 분할 INSERT 2행째의 DIRECT_USE_KEY/CELL_KEY UNIQUE 충돌
                //   (생성컬럼이 REQ_ID NULL + 같은 날 행을 1건으로 강제) 원천 회피. F1 만료 제한이
                //   원 부여의 대상일 유효를 보장하므로 구버전(START_DATE UPDATE) 이동과 원장 결과 동치.
                if (target.grantId() == null || target.grantId().isBlank()) {
                    // 부여 미연결 직접사용(이례 데이터) — 재차감 귀속처가 없어 이동 불가.
                    log.warn("직접사용 이동 불가(F4): 원 GRANT 미연결. cmpnyCd={}, leaveId={}", cmpnyCd, target.leaveId());
                    throw new ApiException(AttdErrorCode.ATTD_400_132);
                }
                String newLeaveId = leaveFlowMapper.selectNextLeaveId(cmpnyCd);
                try {
                    attd13Mapper.insertMovedLeaveUse(new MovedLeaveUseInsertCommand(
                            newLeaveId, cmpnyCd, siteCd, userCd, moveLeaveCd, null, target.grantId(),
                            newDate, movedStartTime, newDate, movedEndTime, unit,
                            chargeDays, totalMinutes, target.leaveReason(), target.evidenceFileId(),
                            target.promotionStage(), target.designatorType(), origDesignated, operatorUserCd));
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    // DIRECT_USE_KEY 최종 방어선(schema-ref ★) — 사전 검증(countLeaveUseOnDate) 이후 경합 시 충돌로 변환.
                    throw new ApiException(AttdErrorCode.ATTD_400_126);
                }
                leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, target.grantId(), operatorUserCd);
            } else {
                // 6-b) 일반 재차감(REQ 연결 건): PC-02 만료임박순 다부여 할당(신청 경로 로직 재사용 — 단일 출처)
                //   + 승계 INSERT(F6: EVIDENCE_FILE_ID 포함)
                List<GrantCharge> charges;
                try {
                    charges = leaveFlowServiceImpl.resolveGeneralCharges(cmpnyCd, userCd, moveLeaveCd, newDate, chargeDays);
                } catch (ApiException e) {
                    if (e.getErrorCode() == AttdErrorCode.ATTD_400_051) {
                        // FOR UPDATE 재판정 잔여 부족(TOCTOU) — 재차감 거부 단일 사유(ATTD_400_132)로 수렴
                        throw new ApiException(AttdErrorCode.ATTD_400_132);
                    }
                    throw e;
                }
                boolean firstCharge = true;
                for (GrantCharge charge : charges) {
                    String newLeaveId = leaveFlowMapper.selectNextLeaveId(cmpnyCd);
                    try {
                        attd13Mapper.insertMovedLeaveUse(new MovedLeaveUseInsertCommand(
                                newLeaveId, cmpnyCd, siteCd, userCd, moveLeaveCd, reqId, charge.grantId(),
                                newDate, movedStartTime, newDate, movedEndTime, unit,
                                charge.days(), firstCharge ? totalMinutes : null, target.leaveReason(),
                                target.evidenceFileId(), target.promotionStage(), target.designatorType(),
                                origDesignated, operatorUserCd));
                    } catch (org.springframework.dao.DuplicateKeyException e) {
                        // REQ 연결 건은 생성컬럼 키 비대상이나, 예기치 못한 UNIQUE 경합도 동일하게 충돌로 변환.
                        throw new ApiException(AttdErrorCode.ATTD_400_126);
                    }
                    if (charge.grantId() != null && !charge.grantId().isEmpty()) {
                        leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, charge.grantId(), operatorUserCd);
                    }
                    firstCharge = false;
                }
            }

            // 7) 훅: 시간차면 원일자·대상일 양쪽 재정산(같은 키 재진입 — 잠금 순서 불변),
            //    이후 짜투리 회수(4단계 cover 무효화 선행으로 자기 cover 부활 없음).
            if (hourly) {
                leaveHourlyResettleService.resettleHourlyLeaveOnDate(cmpnyCd, siteCd, userCd, target.startDate(), operatorUserCd);
                if (!newDate.equals(target.startDate())) {
                    leaveHourlyResettleService.resettleHourlyLeaveOnDate(cmpnyCd, siteCd, userCd, newDate, operatorUserCd);
                }
            }
            leaveRemnantCoverService.reclaimIfPossible(cmpnyCd, userCd, operatorUserCd);

            log.info("연차 이동 재차감 반영. cmpnyCd={}, reqId={}, 원대표행={}, {}→{}, 종류={}, 단위={}, 요금={}, 재발동={}",
                    cmpnyCd, reqId, target.leaveId(), target.startDate(), newDate, moveLeaveCd, unit,
                    chargeDays.toPlainString(), remnantTriggered);
            // prafta-com-008-E-2: 출근 차단은 leave_use(START_DATE) 기준 → 취소+재INSERT 만으로 차단일 이동.
        } finally {
            // afterCompletion 등록 성공분은 여기서 해제하지 않는다(이중 해제 방지). 등록 실패분만 폴백.
            for (Map.Entry<String, Boolean> held : heldLocks.entrySet()) {
                if (!held.getValue()) {
                    releaseMoveLock(held.getKey());
                }
            }
        }
    }

    /**
     * DELETE 반영: 사용행 soft cancel + 부여 USED_DAYS 재계산(차감 복원).
     *
     * <p>T2 확장: 대상이 REQ 연결 건(분할 묶음 포함)이면 REQ 스코프로 전 행 취소 + 다부여 전부
     * 재계산(restoreLeaveLedger 패턴 미러) + <b>자기 cover 무효화(CANCELLED)를 reclaim 훅 전에</b>
     * 수행한다(§0-1-2 — 취소 직후 회수 훅이 그 REQ 로 새 use 행을 만들어 삭제한 휴가가 부활하는
     * 경로 봉합). 직접사용(REQ_ID NULL)은 기존 단건 경로 유지(변화 없음).
     *
     * <p>F9(qa D-4): REQ 경로에 가불 GRANT 회수 추가 — 복원 후 USED_DAYS=0 인 가불 GRANT 만
     * 회수(05 반려 경로와 원장 결과 대칭). 이동 경로는 미적용.
     */
    private void applyDelete(String cmpnyCd, LeaveUseTargetResult target, String operatorUserCd) {
        String reqId = (target.reqId() != null && !target.reqId().isBlank()) ? target.reqId() : null;
        if (reqId != null) {
            // T2: REQ 스코프 확장 — 취소 전 영향 부여 선조회(restoreLeaveLedger 패턴 미러)
            List<String> grantIds = leaveFlowMapper.selectGrantIdsByReqId(cmpnyCd, reqId);
            leaveFlowMapper.cancelLeaveUseByReqId(cmpnyCd, reqId, CANCEL_REASON_CHANGE_DELETE, operatorUserCd);
            for (String grantId : grantIds) {
                if (grantId != null && !grantId.isEmpty()) {
                    leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, grantId, operatorUserCd);
                }
            }
            // F9(qa D-4, 사용자 승인): 이 REQ 가 만든 가불 GRANT 회수 — 05 반려(restoreLeaveLedger)와
            //   동일하게 cancelBorrowGrantByReqId 재사용(기존 statement 무수정 — 술어가 "복원 후
            //   USED_DAYS=0 인 가불 GRANT 만"을 보장). 타 신청이 소비 중(USED_DAYS>0)인 가불 부여는
            //   건너뛰고 보고만 남긴다. 이동(applyMove) 경로는 미적용 — 재차감이 잔존 가불 부여를
            //   자연 재소비하므로 회수하면 이동이 불가해진다.
            int recalledBorrow = leaveGrantEngineService.cancelBorrowGrantByReqId(cmpnyCd, reqId, operatorUserCd);
            if (recalledBorrow > 0) {
                log.info("연차 삭제(동의 확정) — 가불 GRANT 회수 {}건. cmpnyCd={}, reqId={}",
                        recalledBorrow, cmpnyCd, reqId);
            }
            List<String> skippedBorrowGrantIds = attd13Mapper.selectRemainingBorrowGrantIdsByReq(cmpnyCd, reqId);
            if (skippedBorrowGrantIds != null && !skippedBorrowGrantIds.isEmpty()) {
                log.info("연차 삭제(동의 확정) — 가불 GRANT 회수 건너뜀(타 신청 소비분 잔존 USED_DAYS>0) {}건: {}. cmpnyCd={}, reqId={}",
                        skippedBorrowGrantIds.size(), skippedBorrowGrantIds, cmpnyCd, reqId);
            }
            // 자기 cover 무효화(회수 use INSERT 없이 상태만) — 반드시 reclaimIfPossible 전에.
            leaveRemnantCoverService.cancelCoversByReq(cmpnyCd, reqId, operatorUserCd);
        } else {
            attd13Mapper.cancelLeaveUse(cmpnyCd, target.leaveId(), CANCEL_REASON_CHANGE_DELETE, operatorUserCd);
            // 차감 복원: 연결된 부여의 USED_DAYS 를 잔존 CONFIRMED 합계로 재계산(소멸임박 grant 영향은 자연 반영)
            if (target.grantId() != null && !target.grantId().isBlank()) {
                leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, target.grantId(), operatorUserCd);
            }
        }
        // LC-05(F1): 삭제 대상이 시간차(02/03/04)면 그날 잔존 시간차 건을 시간순 재적용해
        //   하한 차액 배치를 보정한다(잔존 건 LEAVE_DAYS 재산출 + 영향 GRANT 재집계).
        if (isHourlyUnit(target.useUnitType())) {
            leaveHourlyResettleService.resettleHourlyLeaveOnDate(
                    cmpnyCd, target.siteCd(), target.userCd(), target.startDate(), operatorUserCd);
        }
        // PC-06(D7): 삭제로 잔여가 복원됐다 — 미도래(근무일 > 오늘) 짜투리 보전 건을 복원 잔여
        //   한도 내에서 정상 차감으로 전환한다(부분 회수 허용, 당일=도래 유지).
        leaveRemnantCoverService.reclaimIfPossible(cmpnyCd, target.userCd(), operatorUserCd);
        // prafta-com-008-E-2: 출근 차단은 leave_use 기준 → soft cancel 로 자동 해제(work_plan SCH_CD 유지).
    }

    /**
     * T1: 이동 반영 advisory lock 획득 + afterCompletion 해제 등록(등록 실패 시 호출부 finally 폴백).
     * 타임아웃/오류 시 동시 처리로 보고 ATTD_409_071(§2-1 관례).
     */
    private void acquireMoveLock(String lockKey, Map<String, Boolean> heldLocks) {
        Integer got = leaveFlowMapper.getAdvisoryLock(lockKey, MOVE_LOCK_TIMEOUT_SEC);
        if (got == null || got != 1) {
            log.info("연차 이동 advisory lock 미획득 — lockKey={}, got={}", lockKey, got);
            throw new ApiException(AttdErrorCode.ATTD_409_071);
        }
        boolean deferred = AdvisoryLockTxUtils.deferReleaseToAfterCompletion(lockKey, this::releaseMoveLock);
        heldLocks.put(lockKey, deferred);
    }

    /** T1: advisory lock 해제(예외 무시 — 세션 종료 시 자동 해제됨). */
    private void releaseMoveLock(String lockKey) {
        try {
            leaveFlowMapper.releaseAdvisoryLock(lockKey);
        } catch (Exception e) {
            log.warn("연차 이동 advisory lock 해제 실패(무시) — lockKey={}", lockKey, e);
        }
    }

    /** null-safe BigDecimal(0 폴백). */
    private BigDecimal nz(BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
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
