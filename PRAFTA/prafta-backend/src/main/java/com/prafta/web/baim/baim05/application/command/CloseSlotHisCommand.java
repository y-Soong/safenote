package com.prafta.web.baim.baim05.application.command;

/**
 * PRAFTA-055-1 — 일일계정 슬롯 점유 해제 이력 UPDATE 커맨드(슬롯 PK 3키의 열린 행 1건을 닫는다).
 * RELEASE_TYPE 은 [SYS016] 코드값(01=관리자 점유해제, 02=사용기간 만료).
 */
public record CloseSlotHisCommand(
    String cmpnyCd
    , String siteCd
    , String slotNo
    , String releaseUser
    , String releaseType
    , String releaseReason
) {
}
