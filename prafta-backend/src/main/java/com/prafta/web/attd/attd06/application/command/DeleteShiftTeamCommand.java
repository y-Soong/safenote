package com.prafta.web.attd.attd06.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.DeleteShiftTeamParam;

public record DeleteShiftTeamCommand(
    String siteCd
    , String shiftCd
    , String shiftTeamId
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static DeleteShiftTeamCommand from(DeleteShiftTeamParam param) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - DeleteShiftTeamParam");

        return new DeleteShiftTeamCommand(
            param.siteCd()
            , param.shiftCd()
            , param.shiftTeamId()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
