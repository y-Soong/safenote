package com.prafta.web.risk.risklink01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risklink01.application.param.LinkedNearMissParam;

/**
 * L2 연결됨 목록 조회 쿼리.
 */
public record LinkedNearMissQuery(
    String gvCmpnyCd
    , String siteCd
    , String processCd
    , String assessmentCd
){
    public static LinkedNearMissQuery from(LinkedNearMissParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new LinkedNearMissQuery(
            param.gvCmpnyCd()
            , param.siteCd()
            , param.processCd()
            , param.assessmentCd()
        );
    }
}
