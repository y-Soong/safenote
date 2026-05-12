package com.prafta.web.attd.attd06.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.UpdateShiftTeamPeriodParam;

public record UpdateShiftTeamPeriodCommand(
    String siteCd
    , String shiftCd
    , String shiftTeamId
    , String strDate
    , String endDate
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static UpdateShiftTeamPeriodCommand from(UpdateShiftTeamPeriodParam param) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - UpdateShiftTeamPeriodParam");

        return new UpdateShiftTeamPeriodCommand(
            param.siteCd()
            , param.shiftCd()
            , param.shiftTeamId()
            , param.strDate()
            , param.endDate()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
