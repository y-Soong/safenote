package com.prafta.common.cmm.dailyjoin.application.command;

/**
 * 일일사용자 회원가입 - 약관 동의 이력(TB_TERMS_USER_AGR_MGMT) insert 커맨드.
 */
public record TermsUserAgrCommand(
    String userCd
    , String termsId
    , String termsVersion
    , String agrYn
) {
    public static TermsUserAgrCommand of(String userCd, String termsId, String termsVersion) {
        return new TermsUserAgrCommand(
            userCd
            , termsId
            , termsVersion
            , "Y"
        );
    }
}
