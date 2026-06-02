package com.prafta.web.attd.attd06.application.model;

public record SaveShiftTeamUserModel(
    String siteCd
    , String nodeCd
    , String teamIdx
    , String userCd
    , String shiftCd
    , String strDate
    , String endDate
    , String gvCmpnyCd
    , String gvUserCd
) {
}
