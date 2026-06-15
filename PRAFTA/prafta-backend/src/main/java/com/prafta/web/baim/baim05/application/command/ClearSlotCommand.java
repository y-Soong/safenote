package com.prafta.web.baim.baim05.application.command;

/**
 * 슬롯 비우기 단건 명령. userCd 는 서버가 슬롯 CURR_USER_CD 로 재조회한 점유자(없으면 null).
 */
public record ClearSlotCommand(
    String gvCmpnyCd
    , String siteCd
    , String slotNo
    , String userCd
    , String gvUserCd
){
}
