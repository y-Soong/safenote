package com.prafta.web.attd.attd06.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.ShiftTypeDetailListsParam;

public record ShiftTypeDetailListsQuery(
    String siteCd
    , String shiftCd
    , String gvCmpnyCd
) {
    public static ShiftTypeDetailListsQuery from(ShiftTypeDetailListsParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ShiftTypeDetailListsQuery(
            param.siteCd()
            , param.shiftCd()
            , param.gvCmpnyCd()
        );
    }
}
