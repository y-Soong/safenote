package com.prafta.web.attd.attd06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.dto.request.ShiftTypeDetailListsRequest;

public record ShiftTypeDetailListsParam(
    String siteCd
    , String shiftCd
    , String gvCmpnyCd
) {
    public static ShiftTypeDetailListsParam from(ShiftTypeDetailListsRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - ShiftTypeDetailListsRequest");

        return new ShiftTypeDetailListsParam(
            request.getSiteCd()
            , request.getShiftCd()
            , tokenInfo.gv_cmpnyCd()
        );
    }
}
