package com.prafta.web.baim.baim05.application.command;

/**
 * 슬롯 구분(SLOT_TYPE, SYS014) 변경 단건 명령. 비점유 슬롯만 대상.
 */
public record SetSlotTypeCommand(
    String cmpnyCd
    , String siteCd
    , String slotNo
    , String slotType
    , String gvUserCd
){
}
