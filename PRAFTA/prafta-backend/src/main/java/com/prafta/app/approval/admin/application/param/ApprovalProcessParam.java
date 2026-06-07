package com.prafta.app.approval.admin.application.param;

import com.prafta.app.approval.admin.dto.request.ApprovalProcessRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 001-P2-B4: 앱 관리자 승인 처리(A-3) Param. plan §3-D 계약.
 *
 * <p>식별자(cmpny/site/user/auth)는 토큰에서만 운반한다(IDOR 차단). reqId 는 리소스 키이며
 * 서비스가 토큰 스코프 내인지 재검증한다.
 *
 * <p>decision 화이트리스트 검증 + REJECT 코멘트 길이(10자 이상) 서버 재검증을 from 단계에서 수행한다.
 * (LEAVE approvalStep 필수 검증은 그룹 확정 후 서비스 디스패치에서 수행한다.)
 */
public record ApprovalProcessParam(
      String reqId
    , String group
    , String decision
    , Integer approvalStep
    , String comment
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {

    public static final String DECISION_APPROVE_ASIS = "APPROVE_ASIS";
    public static final String DECISION_APPROVE_ADJUST = "APPROVE_ADJUST";
    public static final String DECISION_REJECT = "REJECT";

    /** 반려 사유 최소 길이(plan §3-D — REJECT 시 10자 이상). */
    private static final int REJECT_COMMENT_MIN = 10;

    public static ApprovalProcessParam of(ApprovalProcessRequest request, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (request == null || request.getReqId() == null || request.getReqId().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        String decision = request.getDecision() == null ? null : request.getDecision().trim().toUpperCase();
        if (!DECISION_APPROVE_ASIS.equals(decision)
                && !DECISION_APPROVE_ADJUST.equals(decision)
                && !DECISION_REJECT.equals(decision)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        String comment = request.getComment() == null ? null : request.getComment().trim();
        if (DECISION_REJECT.equals(decision) && (comment == null || comment.length() < REJECT_COMMENT_MIN)) {
            // 반려 사유 필수(서버 재검증, 프론트 최소길이와 독립).
            throw new ApiException(AttdErrorCode.ATTD_400_057);
        }

        String group = (request.getGroup() == null || request.getGroup().isBlank())
                ? null : request.getGroup().trim().toUpperCase();

        return new ApprovalProcessParam(
              request.getReqId().trim()
            , group
            , decision
            , request.getApprovalStep()
            , comment
            , token.gv_cmpnyCd()
            , token.gv_userCd()
            , token.gv_siteCd()
            , token.gv_authCd()
        );
    }
}
