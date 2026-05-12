package com.prafta.web.attd.attd06.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.ShiftSchInfosParam.ShiftMetaParam;

public record ShiftTeamCommand(
    String shiftCd
    , String siteCd
    , String shiftTeamId
    , String shiftTeamNm
    , String startDate
    , String endDate
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static ShiftTeamCommand from(
        ShiftMetaParam shiftMetaParam
        , String shiftTeamId
        , String gvCmpnyCd
        , String gvUserCd
    ) {
        if (shiftMetaParam == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - ShiftMetaParam");

        return new ShiftTeamCommand(
            shiftMetaParam.shiftCd()
            , shiftMetaParam.siteCd()
            , shiftTeamId
            , shiftMetaParam.shiftTeamNm()
            , shiftMetaParam.startDate()
            , shiftMetaParam.endDate()
            , gvCmpnyCd
            , gvUserCd
        );
    }
}