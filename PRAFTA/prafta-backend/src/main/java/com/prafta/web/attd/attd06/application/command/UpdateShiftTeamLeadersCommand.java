package com.prafta.web.attd.attd06.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.UpdateShiftTeamLeadersParam;

public record UpdateShiftTeamLeadersCommand(
    String siteCd
    , String userCd
    , String leaderYn
    , String shiftCd
    , String shiftTeamId
    , String teamIdx
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static UpdateShiftTeamLeadersCommand from(UpdateShiftTeamLeadersParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UpdateShiftTeamLeadersCommand(
            param.siteCd()
            , param.userCd()
            , param.leaderYn()
            , param.shiftCd()
            , param.shiftTeamId()
            , param.teamIdx()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
