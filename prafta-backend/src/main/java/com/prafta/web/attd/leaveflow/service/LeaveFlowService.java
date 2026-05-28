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
}
