package com.prafta.app.leave.approval.application.param;

import com.prafta.app.leave.approval.dto.request.LeaveApprovalProcessRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 사용자연차결재-01 (3-D 처리): 연차 결재 단계 승인/반려 Param.
 *
 * <p>연차는 조정 불가 → 승인/반려 2지선다(재기획서 §5.8.4). 식별자(cmpny/user)는 토큰에서만 운반한다(IDOR).
 * decision 화이트리스트 + REJECT 사유 비공란을 from 단계에서 1차 검증한다(서버 단일 출처는 엔진 ATTD_400_057).
 * 실제 처리는 LeaveFlowService.approveStep/rejectStep 위임 — 멱등/단계차례/본인결재/마감은 엔진이 재검증한다.
 */
public record LeaveApprovalProcessParam(
      String reqId
    , Integer approvalStep
    , String decision
    , String comment
    , String gvCmpnyCd
    , String gvUserCd
) {

    public static final String DECISION_APPROVE = "APPROVE";
    public static final String DECISION_REJECT = "REJECT";

    public static LeaveApprovalProcessParam of(LeaveApprovalProcessRequest request, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (request == null || request.getReqId() == null || request.getReqId().isBlank()
                || request.getApprovalStep() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        String decision = request.getDecision() == null ? null : request.getDecision().trim().toUpperCase();
        if (!DECISION_APPROVE.equals(decision) && !DECISION_REJECT.equals(decision)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        String comment = request.getComment() == null ? null : request.getComment().trim();
        // 반려 사유 필수(이력 보존 §9.5). 엔진(ATTD_400_057)과 별개로 진입 단계에서도 비공란을 강제한다.
        if (DECISION_REJECT.equals(decision) && (comment == null || comment.isEmpty())) {
            throw new ApiException(AttdErrorCode.ATTD_400_057);
        }

        return new LeaveApprovalProcessParam(
              request.getReqId().trim()
            , request.getApprovalStep()
            , decision
            , comment
            , token.gv_cmpnyCd()
            , token.gv_userCd()
        );
    }
}
