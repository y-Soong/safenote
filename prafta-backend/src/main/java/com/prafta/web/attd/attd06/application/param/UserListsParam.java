package com.prafta.web.attd.attd06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.dto.request.UserListsRequest;

public record UserListsParam(
    String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String gvCmpnyCd
) {
    public static UserListsParam from(UserListsRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - UserListsRequest");

        return new UserListsParam(
            request.getSiteCd()
            , request.getNodeCd()
            , request.getIncSubNodeYn()
            , tokenInfo.gv_cmpnyCd()
        );
    }
}
