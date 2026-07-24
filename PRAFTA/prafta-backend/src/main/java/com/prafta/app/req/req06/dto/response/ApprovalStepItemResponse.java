package com.prafta.app.req.req06.dto.response;

/**
 * PRAFTA-내승인요청결재라인-1: 결재라인 상세 단계 1건.
 *
 * <p>{@link com.prafta.common.cmm.approval.vo.ApprovalStepVO} 를 목록 카드 상태(SYS033)와
 * 코드 레벨에서부터 구분하기 위해 필드명을 리네임하여 매핑한다(stepStatus/stepStatusDisplay).
 *
 * @param approvalStep      결재 단계 (1부터)
 * @param approverUserNm    결재자명
 * @param stepStatus        단계 상태 코드 [SYS044] 00대기/01신청/02승인/03반려
 * @param stepStatusDisplay 단계 상태명
 * @param approvalComment   결재 코멘트
 * @param approvalDate      처리 일시 (ISO 8601), 미처리 단계는 null
 */
public record ApprovalStepItemResponse(
        Integer approvalStep
        , String approverUserNm
        , String stepStatus
        , String stepStatusDisplay
        , String approvalComment
        , String approvalDate
) {
}
