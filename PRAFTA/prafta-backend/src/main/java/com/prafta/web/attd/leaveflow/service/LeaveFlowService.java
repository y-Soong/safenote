package com.prafta.web.attd.leaveflow.service;

import com.prafta.web.attd.leaveflow.application.param.LeaveApplyParam;
import com.prafta.web.attd.leaveflow.application.param.LeaveApprovalActionParam;

/**
 * 연차 신청·결재 흐름 서비스 (prafta-019-E).
 *
 * <p>A(차감 유틸 LeaveDeductionService)·C(근태 마감 isClosed)·D(결재라인 ApprovalLineMapper)를 조립한다.
 * 차감 모델: "신청 시 예약(tb_user_leave_use CONFIRMED) → 반려 시 해제(CANCELLED)".
 */
public interface LeaveFlowService {

    /** 연차 신청: 검증(시간차/사후마감) → 부여 차감 예약 → 결재 Y면 라인 생성 / N이면 즉시 확정. */
    void submitLeave(LeaveApplyParam param);

    /** 결재 단계 승인 (지정 결재자 본인만, 순서 강제). 마지막 단계 승인 시 요청 확정. */
    void approveStep(LeaveApprovalActionParam param);

    /** 결재 단계 반려 → 요청 반려 + 차감 해제. */
    void rejectStep(LeaveApprovalActionParam param);

    /** 내 결재함: 내가 현재 단계 결재자인 연차 요청 목록 (요청승인관리 연차 탭). */
    java.util.List<com.prafta.web.attd.leaveflow.vo.MyLeaveApprovalVO> getMyPendingLeaveApprovals(
            String cmpnyCd, String approverUserCd);

    /**
     * 결재 없이 연차 1일을 즉시 사용 기록(차감)한다 — 근무계획 관리(Attd_05) 관리자 직접 적용용(prafta-021).
     *
     * <p>요청/결재(TB_USER_ATTD_REQ·APPROVAL) 없이 차감 예약(tb_user_leave_use CONFIRMED, REQ_ID 없음)
     * + 부여 USED_DAYS 동기화만 수행한다. 일 단위(UNIT '00', 1일) 고정. 동일 직원·일자·연차코드로
     * 이미 기록됐으면 멱등 skip(중복 차감 방지), 차감 가능 부여가 없으면 INSUFFICIENT.
     *
     * @return {@link com.prafta.web.attd.leaveflow.vo.DirectLeaveResult}
     */
    com.prafta.web.attd.leaveflow.vo.DirectLeaveResult recordDirectLeaveUsage(
            String cmpnyCd, String siteCd, String userCd, String workYmd, String leaveCd, String operatorUserCd);

    /**
     * 결재 없이 기록된 직접 연차 사용(근무계획 직접 적용, {@link #recordDirectLeaveUsage})을 셀 단위로 취소하고
     * 부여 잔여를 복원한다 — 근무계획 관리(Attd_05)에서 연차 셀을 비울 때 호출한다(prafta-041).
     *
     * <p>{@code recordDirectLeaveUsage} 의 역연산이다. 대상은 동일 직원·일자(START_DATE)·연차코드의
     * REQ_ID 없음 + LEAVE_STATUS='CONFIRMED' + 미삭제(DEL_YN='N') 직접 사용기록이며,
     * 이를 soft cancel(LEAVE_STATUS='CANCELLED' + CANCEL_REASON/CANCEL_DATE/UPDATE_*) 처리하여
     * 사용 이력을 보존(§8.5.8 "사용 이력 삭제 금지")한 뒤, 연결된 부여(GRANT)의 USED_DAYS 를
     * 잔존 CONFIRMED 합계로 재계산하여 잔여를 복원한다. 결재 경유(REQ_ID 존재) 건은 대상이 아니다.
     *
     * <p>대상이 없으면 no-op(이미 취소/미존재). 방어적으로 다건이 매칭되면 모두 취소하고
     * 영향받은 부여를 각각 재계산한다.
     *
     * @return 취소된 직접 사용기록 행 수 (0이면 no-op)
     */
    int cancelDirectLeaveUsage(
            String cmpnyCd, String userCd, String workYmd, String leaveCd, String operatorUserCd);
}
