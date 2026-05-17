package com.prafta.web.attd.attd06.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.UpdateShiftTeamNmParam;

public record UpdateShiftTeamNmCommand(
    String siteCd
    , String shiftCd
    , String shiftTeamId
    , String shiftTeamNm
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static UpdateShiftTeamNmCommand from(UpdateShiftTeamNmParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UpdateShiftTeamNmCommand(
            param.siteCd()
            , param.shiftCd()
            , param.shiftTeamId()
            , param.shiftTeamNm()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
