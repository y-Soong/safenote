package com.prafta.web.attd.attd06.application.command;

import java.util.List;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.param.ShiftSchInfosParam.MemberParam;
import com.prafta.web.attd.attd06.application.param.ShiftSchInfosParam.ShiftMetaParam;
import com.prafta.web.attd.attd06.application.param.ShiftSchInfosParam.TeamParam;

public record ShiftTeamUserCommand(
    String teamIdx
    , String shiftCd
    , String siteCd
    , String shiftTeamId
    , List<MemberParam> memberList
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static ShiftTeamUserCommand from(
        TeamParam teamParam
        , ShiftMetaParam shiftMetaParam
        , String shiftTeamId
        , String gvCmpnyCd
        , String gvUserCd
    ) {
        if (teamParam == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - TeamParam");
        if (shiftMetaParam == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - ShiftMetaParam");
        if (teamParam.memberList() == null || teamParam.memberList().isEmpty())
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - MemberList");

        return new ShiftTeamUserCommand(
            teamParam.teamIdx()
            , shiftMetaParam.shiftCd()
            , shiftMetaParam.siteCd()
            , shiftTeamId
            , teamParam.memberList()
            , gvCmpnyCd
            , gvUserCd
        );
    }
}