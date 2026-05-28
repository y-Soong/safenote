package com.prafta.web.user.user01.application.command;

/**
 * 경력 인정 항목 INSERT 커맨드 (PRAFTA-017-4).
 * CREDIT_ID는 mapper에서 FNC_CMM_SEQ_NEXTVAL로 채번한다.
 * REASON_TYPE은 화면에서 사유 유형을 입력하지 않으므로 'OTHER'를 기본 저장한다([SYS042], NOT NULL).
 */
public record UserCreditInsertCommand(
    String cmpnyCd
    , String userCd
    , Integer creditMonths
    , String reasonType
    , String reasonDetail
    , String gvCmpnyCd
    , String gvUserCd
) {
    private static final String DEFAULT_REASON_TYPE = "OTHER";

    public static UserCreditInsertCommand of(
        String cmpnyCd
        , String userCd
        , Integer creditMonths
        , String reasonDetail
        , String gvCmpnyCd
        , String gvUserCd
    ) {
        return new UserCreditInsertCommand(
            cmpnyCd
            , userCd
            , creditMonths
            , DEFAULT_REASON_TYPE
            , reasonDetail
            , gvCmpnyCd
            , gvUserCd
        );
    }
}
