package com.prafta.web.baim.baim05.application.command;

/**
 * 슬롯 점유 고정여부 토글 단건 명령. 점유 슬롯(SLOT_STATUS='02')만 대상.
 */
public record SetSlotFixedCommand(
    String gvCmpnyCd
    , String siteCd
    , String slotNo
    , String fixedYn
    , String gvUserCd
){
}
