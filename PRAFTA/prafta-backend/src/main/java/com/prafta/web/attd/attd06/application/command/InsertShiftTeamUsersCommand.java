package com.prafta.web.attd.attd06.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.model.InsertShiftTeamUsersModel;

public record InsertShiftTeamUsersCommand(
    String siteCd
    , String shiftCd
    , String shiftTeamId
    , String teamIdx
    , String userCd
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static InsertShiftTeamUsersCommand from(InsertShiftTeamUsersModel model) {

        if (model == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new InsertShiftTeamUsersCommand(
            model.siteCd()
            , model.shiftCd()
            , model.shiftTeamId()
            , model.teamIdx()
            , model.userCd()
            , model.gvCmpnyCd()
            , model.gvUserCd()
        );
    }
}
