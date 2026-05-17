package com.prafta.web.attd.attd06.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.ShiftTypeListsParam;

public record ShiftTypeListsQuery(
    String siteCd
    , String gvCmpnyCd
) {
    public static ShiftTypeListsQuery from(ShiftTypeListsParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ShiftTypeListsQuery(
            param.siteCd()
            , param.gvCmpnyCd()
        );
    }
}
