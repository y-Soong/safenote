package com.prafta.web.attd.attd06.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.ShiftTeamUserInfosParam;

public record ShiftTeamUserInfosQuery(
    String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String shiftCd
    , String gvCmpnyCd
    , String gvAuthCd
) {
    public static ShiftTeamUserInfosQuery from(ShiftTeamUserInfosParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ShiftTeamUserInfosQuery(
            param.siteCd()
            , param.nodeCd()
            , param.incSubNodeYn()
            , param.shiftCd()
            , param.gvCmpnyCd()
            , param.gvAuthCd()
        );
    }
}
