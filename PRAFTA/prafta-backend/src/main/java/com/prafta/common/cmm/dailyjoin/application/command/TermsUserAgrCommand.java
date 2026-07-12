package com.prafta.common.cmm.dailyjoin.application.command;

/**
 * 일일사용자 회원가입 - 약관 동의 이력(TB_TERMS_USER_AGR_MGMT) insert 커맨드.
 */
public record TermsUserAgrCommand(
    String cmpnyCd
    , String userCd
    , String termsId
    , String termsVersion
    , String agrYn
) {
    // cmpnyCd 는 일용직 가입 흐름의 회사(joinCd 도출)로, 생성되는 TB_USER/TB_DAILY_USER 와 동일 회사 스코프.
    public static TermsUserAgrCommand of(String cmpnyCd, String userCd, String termsId, String termsVersion) {
        return new TermsUserAgrCommand(
            cmpnyCd
            , userCd
            , termsId
            , termsVersion
            , "Y"
        );
    }
}
