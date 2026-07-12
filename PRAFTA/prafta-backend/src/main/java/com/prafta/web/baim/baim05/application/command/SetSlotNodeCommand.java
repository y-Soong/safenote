package com.prafta.web.baim.baim05.application.command;

/**
 * 슬롯 소속부서(NODE_CD) 지정/해제 단건 명령.
 * nodeCd 가 null 이면 부서 해제(NODE_CD = NULL). 점유중 슬롯도 대상(잠금 없음).
 */
public record SetSlotNodeCommand(
    String cmpnyCd
    , String siteCd
    , String slotNo
    , String nodeCd
    , String gvUserCd
){
}
