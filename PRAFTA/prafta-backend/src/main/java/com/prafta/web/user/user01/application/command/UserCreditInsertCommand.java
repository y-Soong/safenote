package com.prafta.web.user.user01.application.command;

import java.math.BigDecimal;

/**
 * 경력 인정 항목 INSERT 커맨드 (PRAFTA-017-4).
 * CREDIT_ID는 mapper에서 FNC_CMM_SEQ_NEXTVAL로 채번한다.
 *
 * <p>경력인정 이원화(2026-08-21, 지시서 §1-1): reasonType/leaveCalcYn/extraLeaveDays는
 * 서비스 레이어(User01ServiceImpl)가 이미 정규화·검증(허용코드/Y·N/0.5단위·상한 25)한 값을
 * 그대로 받는다 — 본 record 는 하드코딩 기본값(구 'OTHER' 고정)을 두지 않는다.
 */
public record UserCreditInsertCommand(
    String cmpnyCd
    , String userCd
    , Integer creditMonths
    , String reasonType
    , String reasonDetail
    , String leaveCalcYn
    , BigDecimal extraLeaveDays
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static UserCreditInsertCommand of(
        String cmpnyCd
        , String userCd
        , Integer creditMonths
        , String reasonType
        , String reasonDetail
        , String leaveCalcYn
        , BigDecimal extraLeaveDays
        , String gvCmpnyCd
        , String gvUserCd
    ) {
        return new UserCreditInsertCommand(
            cmpnyCd
            , userCd
            , creditMonths
            , reasonType
            , reasonDetail
            , leaveCalcYn
            , extraLeaveDays
            , gvCmpnyCd
            , gvUserCd
        );
    }
}
