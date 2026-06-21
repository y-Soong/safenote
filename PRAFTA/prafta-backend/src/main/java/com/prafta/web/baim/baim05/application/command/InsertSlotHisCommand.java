package com.prafta.web.baim.baim05.application.command;

/**
 * PRAFTA-055-1 — 일일계정 슬롯 점유 시작 이력(TB_DAILY_USER_SLOT_HIS) INSERT 커맨드.
 * OCCUPY_DTIME=NOW(), RELEASE_* 은 NULL(점유 시작 시점). 해제 시 closeSlotHis 로 열린 행을 닫는다.
 */
public record InsertSlotHisCommand(
    String hisId
    , String cmpnyCd
    , String siteCd
    , String slotNo
    , String workDate
    , String userId
    , String issueChannel
    , String insertNo
) {
}
