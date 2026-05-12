package com.prafta.web.attd.attd06.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.DeleteShiftTeamUserParam;

public record DeleteShiftTeamUserCommand(
    String siteCd
    , String shiftCd
    , String shiftTeamId
    , String teamIdx
    , String userCd
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static DeleteShiftTeamUserCommand from(DeleteShiftTeamUserParam param) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - InsertShiftTeamUserParam");

        return new DeleteShiftTeamUserCommand(
            param.siteCd()
            , param.shiftCd()
            , param.shiftTeamId()
            , param.teamIdx()
            , param.userCd()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
