package com.prafta.web.attd.attd06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.dto.request.ShiftTeamUserInfosRequest;

public record ShiftTeamUserInfosParam(
    String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String shiftCd
    , String gvCmpnyCd
    , String gvAuthCd
) {
    public static ShiftTeamUserInfosParam from(ShiftTeamUserInfosRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - ShiftTeamUserInfosRequest");

        return new ShiftTeamUserInfosParam(
            request.getSiteCd()
            , request.getNodeCd()
            , request.getIncSubNodeYn()
            , request.getShiftCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_authCd()
        );
    }
}
