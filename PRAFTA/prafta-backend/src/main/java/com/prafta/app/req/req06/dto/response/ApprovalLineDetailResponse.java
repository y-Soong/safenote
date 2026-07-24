package com.prafta.app.req.req06.dto.response;

import java.util.List;

/**
 * PRAFTA-내승인요청결재라인-1: 본인 요청 결재라인 상세 조회 응답.
 *
 * @param reqId 조회 대상 요청 ID
 * @param steps 단계별 결재라인 (단계 오름차순, 완료+대기 단계 전부 포함)
 */
public record ApprovalLineDetailResponse(
        String reqId
        , List<ApprovalStepItemResponse> steps
) {
}
