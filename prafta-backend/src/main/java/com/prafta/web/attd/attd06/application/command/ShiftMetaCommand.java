package com.prafta.web.attd.attd06.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.ShiftSchInfosParam;

public record ShiftMetaCommand(
    String siteCd
    , String shiftCd
    , String startDate
    , String endDate
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static ShiftMetaCommand from(ShiftSchInfosParam param) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - SaveShiftSchInfosParam");

        return new ShiftMetaCommand(
            param.shiftMeta().siteCd()
            , param.shiftMeta().shiftCd()
            , param.shiftMeta().startDate()
            , param.shiftMeta().endDate()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
