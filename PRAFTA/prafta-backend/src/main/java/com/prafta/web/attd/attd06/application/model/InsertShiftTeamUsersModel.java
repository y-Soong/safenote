package com.prafta.web.attd.attd06.application.model;

public record InsertShiftTeamUsersModel(
    String siteCd
    , String shiftCd
    , String shiftTeamId
    , String teamIdx
    , String userCd
    , String gvCmpnyCd
    , String gvUserCd
) {
}
