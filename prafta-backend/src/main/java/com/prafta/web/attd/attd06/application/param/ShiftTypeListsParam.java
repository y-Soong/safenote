package com.prafta.web.attd.attd06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.dto.request.ShiftTypeListsRequest;

public record ShiftTypeListsParam(
    String siteCd
    , String gvCmpnyCd
) {
    public static ShiftTypeListsParam from(ShiftTypeListsRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ShiftTypeListsParam(
            request.getSiteCd()
            , tokenInfo.gv_cmpnyCd()
        );
    }
}
