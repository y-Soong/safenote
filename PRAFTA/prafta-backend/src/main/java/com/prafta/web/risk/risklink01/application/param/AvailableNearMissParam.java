package com.prafta.web.risk.risklink01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risklink01.dto.request.AvailableNearMissRequest;

/**
 * L1 연결 후보 검색 파라미터. 식별자는 JWT 에서만 도출.
 */
public record AvailableNearMissParam(
    String siteCd
    , String processCd
    , String assessmentCd
    , String keyword
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static AvailableNearMissParam from(AvailableNearMissRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AvailableNearMissParam(
            request.getSiteCd()
            , request.getProcessCd()
            , request.getAssessmentCd()
            , request.getKeyword()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
