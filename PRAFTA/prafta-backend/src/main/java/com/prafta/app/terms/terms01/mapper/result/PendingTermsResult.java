package com.prafta.app.terms.terms01.mapper.result;

/**
 * 미동의 필수약관 1건(앱 로그인 게이트용).
 *
 * <p>현재버전(TERMS_VERSION) 기준으로 해당 사용자가 AGR_YN='Y' 동의하지 않은 필수약관만 담는다.
 * <p>termsNm 은 SYS008(TB_SYST_VAL_D)의 약관명, termsContent 는 TB_TERMS 본문.
 */
public record PendingTermsResult(
        String termsId
        , String termsNm
        , String termsVersion
        , String termsContent
) {
}
