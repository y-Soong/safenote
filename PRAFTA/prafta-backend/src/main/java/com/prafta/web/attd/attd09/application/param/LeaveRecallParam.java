package com.prafta.web.attd.attd09.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd09.dto.request.LeaveRecallRequest;

/**
 * 관리자 수동 부여 연차 회수(soft cancel) 진입 Param (PRAFTA-031).
 *
 * <p>JWT 클레임(권한/회사/수행자)을 함께 운반하여 서비스 계층이 권한 가드 + 스코프 격리를
 * 수행할 수 있도록 한다(정책서 §8.5.7).
 *
 * <p>cmpnyCd/operatorUserCd는 JWT에서만 취득(요청 body 미신뢰 — 가드레일 3).
 * grantId는 PathVariable로 받는다.
 */
public record LeaveRecallParam(
      String grantId
    , String reason
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {

    /** 회수 요청(PathVariable grantId + body reason + JWT) → Param. */
    public static LeaveRecallParam from(String grantId, LeaveRecallRequest request, TokenInfo tokenInfo) {
        validateToken(tokenInfo);
        if (grantId == null || grantId.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String reason = (request == null) ? null : request.getReason();
        return new LeaveRecallParam(
              grantId
            , reason
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }

    private static void validateToken(TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
    }
}
