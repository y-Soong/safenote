package com.prafta.web.baim.baim05.application.command;

/**
 * 슬롯 기본 근무타입(DEFAULT_SCH_CD) 지정/해제 단건 명령.
 * schCd 가 null 이면 해제(DEFAULT_SCH_CD = NULL, 근로자 본인 선택 폴백). 점유중 슬롯도 대상(잠금 없음).
 */
public record SetSlotSchCommand(
    String cmpnyCd
    , String siteCd
    , String slotNo
    , String schCd
    , String gvUserCd
){
}
