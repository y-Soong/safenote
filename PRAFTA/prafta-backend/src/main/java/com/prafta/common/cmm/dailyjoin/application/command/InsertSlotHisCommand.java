package com.prafta.common.cmm.dailyjoin.application.command;

/**
 * PRAFTA-055-1 — 일일계정 자가가입(직접가입) 슬롯 점유 시작 이력(TB_DAILY_USER_SLOT_HIS) INSERT 커맨드.
 * OCCUPY_DTIME=NOW(), RELEASE_* 은 NULL(점유 시작 시점). 해제는 자정 만료 배치/관리자 비우기에서 닫는다.
 * ISSUE_CHANNEL[SYS014] : 01 = 직접가입.
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
    public static InsertSlotHisCommand of(
        String hisId
        , String cmpnyCd
        , String siteCd
        , String slotNo
        , String workDate
        , String userId
    ) {
        return new InsertSlotHisCommand(
            hisId
            , cmpnyCd
            , siteCd
            , slotNo
            , workDate
            , userId
            , "01"          // ISSUE_CHANNEL : 01 = 직접가입(링크/QR 자가가입)
            , userId        // INSERT_NO : 비로그인 외부 가입이므로 발급된 USER_CD 사용
        );
    }
}
