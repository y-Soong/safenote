package com.prafta.web.risk.risklink01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risklink01.dto.request.NearMissLinkRequest;

/**
 * L3 연결 추가 / L4 연결 해제 공통 파라미터. 식별자는 JWT 에서만 도출.
 */
public record NearMissLinkParam(
    String siteCd
    , String processCd
    , String assessmentCd
    , String nearMissId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static NearMissLinkParam from(NearMissLinkRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new NearMissLinkParam(
            request.getSiteCd()
            , request.getProcessCd()
            , request.getAssessmentCd()
            , request.getNearMissId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
