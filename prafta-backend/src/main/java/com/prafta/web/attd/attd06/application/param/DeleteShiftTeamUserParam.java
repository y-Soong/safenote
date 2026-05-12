package com.prafta.web.attd.attd06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.dto.request.DeleteShiftTeamUserRequest;

public record DeleteShiftTeamUserParam(
    String siteCd
    , String shiftCd
    , String shiftTeamId
    , String teamIdx
    , String userCd
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static DeleteShiftTeamUserParam from(DeleteShiftTeamUserRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - InsertShiftTeamUserRequest");

        return new DeleteShiftTeamUserParam(
            request.getSiteCd()
            , request.getShiftCd()
            , request.getShiftTeamId()
            , request.getTeamIdx()
            , request.getUserCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
