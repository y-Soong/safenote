package com.prafta.web.nearmiss.nearmiss01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.nearmiss.nearmiss01.application.param.IncidentListParam;

public record IncidentListQuery(
    String siteCd
    , String reportStatusCd
    , String potentialSeverityCd
    , String startDate
    , String endDate
    , String gvCmpnyCd
){
    public static IncidentListQuery from(IncidentListParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new IncidentListQuery(
            param.siteCd()
            , param.reportStatusCd()
            , param.potentialSeverityCd()
            , param.startDate()
            , param.endDate()
            , param.gvCmpnyCd()
        );
    }
}
