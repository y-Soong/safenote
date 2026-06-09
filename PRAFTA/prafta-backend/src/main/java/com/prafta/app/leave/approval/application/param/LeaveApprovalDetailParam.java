package com.prafta.app.leave.approval.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 사용자연차결재-01 (3-B 상세): 연차 결재 상세 조회 Param.
 *
 * <p>reqId 는 리소스 키, approvalStep 은 내 단계(advisory — 서버가 결재선에서 내 단계를 재산출한다).
 * 식별자(cmpny/user/site)는 토큰에서만 운반하며 서버가 isApproverOf 로 결재선 실존을 재검증한다(IDOR).
 */
public record LeaveApprovalDetailParam(
      String reqId
    , Integer approvalStep
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
) {
    public static LeaveApprovalDetailParam of(String reqId, Integer approvalStep, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (reqId == null || reqId.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new LeaveApprovalDetailParam(
              reqId.trim()
            , approvalStep
            , token.gv_cmpnyCd()
            , token.gv_userCd()
            , token.gv_siteCd()
        );
    }
}
