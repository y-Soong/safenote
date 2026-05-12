package com.prafta.web.attd.attd06.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.UserListsParam;

public record UserListsQuery(
    String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String gvCmpnyCd
) {
    public static UserListsQuery from(UserListsParam param) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - UserListsParam");

        return new UserListsQuery(
            param.siteCd()
            , param.nodeCd()
            , param.incSubNodeYn()
            , param.gvCmpnyCd()
        );
    }
}
