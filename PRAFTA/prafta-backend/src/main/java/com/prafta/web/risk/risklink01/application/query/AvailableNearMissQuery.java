package com.prafta.web.risk.risklink01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risklink01.application.param.AvailableNearMissParam;

/**
 * L1 연결 후보 검색 쿼리.
 */
public record AvailableNearMissQuery(
    String gvCmpnyCd
    , String siteCd
    , String processCd
    , String assessmentCd
    , String keyword
){
    public static AvailableNearMissQuery from(AvailableNearMissParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AvailableNearMissQuery(
            param.gvCmpnyCd()
            , param.siteCd()
            , param.processCd()
            , param.assessmentCd()
            , param.keyword()
        );
    }
}
