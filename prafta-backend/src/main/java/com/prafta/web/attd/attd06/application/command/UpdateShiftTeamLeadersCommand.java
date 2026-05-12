package com.prafta.web.attd.attd06.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.UpdateShiftTeamLeadersParam;

public record UpdateShiftTeamLeadersCommand(
    String siteCd
    , String userCd
    , String leaderYn
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static UpdateShiftTeamLeadersCommand from(UpdateShiftTeamLeadersParam param) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - UpdateShiftTeamLeadersParam");

        return new UpdateShiftTeamLeadersCommand(
            param.siteCd()
            , param.userCd()
            , param.leaderYn()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
