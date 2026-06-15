package com.prafta.web.risk.risklink01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risklink01.dto.request.LinkedNearMissRequest;

/**
 * L2 연결됨 목록 조회 파라미터. 식별자는 JWT 에서만 도출.
 */
public record LinkedNearMissParam(
    String siteCd
    , String processCd
    , String assessmentCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static LinkedNearMissParam from(LinkedNearMissRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new LinkedNearMissParam(
            request.getSiteCd()
            , request.getProcessCd()
            , request.getAssessmentCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
