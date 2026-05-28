package com.prafta.common.cmm.dailyjoin.application.command;

/**
 * 일일사용자 회원가입 - 빈 슬롯 점유(TB_DAILY_USER_SLOT.CURR_USER_CD 매핑) 커맨드.
 * SLOT_TYPE='01'(직접가입), SLOT_STATUS='02'(점유, baim05 점유값과 동일).
 */
public record DailyUserSlotUpdCommand(
    String cmpnyCd
    , String siteCd
    , String slotNo
    , String userCd
    , String slotType
    , String slotStatus
) {
    public static DailyUserSlotUpdCommand of(String cmpnyCd, String siteCd, String slotNo, String userCd) {
        return new DailyUserSlotUpdCommand(
            cmpnyCd
            , siteCd
            , slotNo
            , userCd
            , "01"      // SLOT_TYPE : 01 = 직접가입(링크 회원가입)
            , "02"      // SLOT_STATUS : 02 = 점유
        );
    }
}
