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

    /**
     * LC-07(T3): 예상 차감액 미리보기 — INSERT 없음(조회 전용).
     *
     * <p>검증 가드(단위 구조/스케줄/휴게 가로지름/사후마감/1.0 점유/시간대 겹침)는 {@link #submitLeave}
     * 와 동일하게 태워 "신청하면 거부될 값"을 미리 보여주지 않는다(위반 시 해당 에러 그대로 반환).
     * 잔여 부족은 에러가 아니라 플래그({@code insufficientBalance})로 내려 FE 가 사전 경고한다.
     * 인가: 본인 신청 기준(JWT gv_userCd)만 — 타인 대상 입력 없음.
     */
    com.prafta.web.attd.leaveflow.dto.response.LeaveDeductionPreviewResponse previewDeduction(
            com.prafta.web.attd.leaveflow.application.param.LeaveDeductionPreviewParam param);

    /** 결재 단계 승인 (지정 결재자 본인만, 순서 강제). 마지막 단계 승인 시 요청 확정. */
    void approveStep(LeaveApprovalActionParam param);

    /** 결재 단계 반려 → 요청 반려 + 차감 해제. */
    void rejectStep(LeaveApprovalActionParam param);

    /**
     * QT-11-7 — 결재 흐름 밖에서 연차 요청이 강제 종료(취소/반려)될 때 연차 원장을 원복한다.
     *
     * <p>소속이동 발효({@code User01TransferExecutionService})처럼 배치가 TB_USER_ATTD_REQ 의 상태만
     *   직접 UPDATE 하는 경로는, 정상 반려({@link #rejectStep})가 수행하는 <b>use 행 취소 + GRANT
     *   USED_DAYS 재집계 + 가불 GRANT 회수 + 시간차 재정산(F1)</b> 을 건너뛰어 <b>차감이 그대로 남는다</b>
     *   (사용자는 쓰지 않은 연차를 잃는다). 그 경로에서 요청 상태를 바꾼 뒤 본 메서드를 호출해
     *   원장을 원복한다. 원복 시퀀스는 {@code rejectStep} 과 단일 출처를 공유한다.
     *
     * <p>REQ_TYPE='06'(연차 수정)은 승인 시에만 반영되므로 되돌릴 차감이 없다 → 무시(no-op).
     *   대상 요청이 없거나 연차 요청이 아니면 아무 것도 하지 않는다(멱등).
     *
     * @param reason use 행 CANCEL_REASON 에 남길 사유(예: "소속이동")
     * @param actor  처리자 USER_CD(감사 컬럼)
     */
    void restoreLeaveLedgerOnTerminate(String cmpnyCd, String reqId, String reason, String actor);

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
     * prafta-com-016-C-4: 종류 미지정 자동 차감 — 후보 법정휴가(연차/월차) 중 <b>소멸 임박 통합순</b>으로
     * 1일을 차감한다(종류 무관, AVAIL_TO_DATE→GRANT_DATE 우선). 근무계획 관리(Attd_05)에서 관리자가
     * "법정 휴가"를 종류 선택 없이 적용할 때 호출한다.
     *
     * <p>{@link #recordDirectLeaveUsage} 와 동일하게 결재 없이 차감 기록(REQ_ID 없음, CONFIRMED, 종일 1일)
     * + 부여 USED_DAYS 동기화를 수행한다. 차이는 차감할 연차코드를 호출자가 지정하지 않고, 후보 집합에서
     * 만료가 가장 임박한 부여를 시스템이 자동 선택한다는 점이다. 해당 셀에 이미 종일 CONFIRMED 연차가
     * 있으면 멱등 skip(SKIPPED_DUP), 후보 전체에 차감 가능 부여가 없으면 INSUFFICIENT.
     *
     * @param candidateLeaveCds 차감 후보 휴가코드(예: [SYS_ANNUAL, SYS_MONTHLY]). 모두 부여 기반(법정) 종류여야 한다.
     * @return {@link com.prafta.web.attd.leaveflow.vo.DirectLeaveResult}
     */
    com.prafta.web.attd.leaveflow.vo.DirectLeaveResult recordDirectLeaveUsageAuto(
            String cmpnyCd, String siteCd, String userCd, String workYmd,
            java.util.List<String> candidateLeaveCds, String operatorUserCd);

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
