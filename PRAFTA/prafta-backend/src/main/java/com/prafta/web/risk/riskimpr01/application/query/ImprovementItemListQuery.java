package com.prafta.web.risk.riskimpr01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementItemListParam;

/**
 * 개선항목 목록 조회 쿼리(평가키 스코프).
 */
public record ImprovementItemListQuery(
    String gvCmpnyCd
    , String siteCd
    , String processCd
    , String assessmentCd
){
    public static ImprovementItemListQuery from(ImprovementItemListParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ImprovementItemListQuery(
            param.gvCmpnyCd()
            , param.siteCd()
            , param.processCd()
            , param.assessmentCd()
        );
    }
}
