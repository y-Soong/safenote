package com.prafta.web.nearmiss.nearmiss01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.nearmiss.nearmiss01.application.param.IncidentInfoParam;

public record IncidentInfoQuery(
    String siteCd
    , String nearMissId
    , String gvCmpnyCd
){
    public static IncidentInfoQuery from(IncidentInfoParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new IncidentInfoQuery(
            param.siteCd()
            , param.nearMissId()
            , param.gvCmpnyCd()
        );
    }
}
