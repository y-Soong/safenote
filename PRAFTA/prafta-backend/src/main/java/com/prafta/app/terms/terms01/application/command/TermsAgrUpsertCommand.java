package com.prafta.app.terms.terms01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 약관 동의 upsert Command(TB_TERMS_USER_AGR_MGMT).
 *
 * <p>PK=(CMPNY_CD,USER_CD,TERMS_ID,TERMS_VERSION) 기준 INSERT ... ON DUPLICATE KEY UPDATE AGR_YN.
 *    필수약관 일괄 동의(AGR_YN='Y')와 선택약관 토글(AGR_YN='Y'|'N') 양쪽이 공유한다.
 * <p>CMPNY_CD/USER_CD/TERMS_VERSION 은 서버가 확정한 값만 사용한다(CMPNY_CD/USER_CD=JWT, TERMS_VERSION=TB_TERMS 현재버전).
 */
public record TermsAgrUpsertCommand(
        String cmpnyCd
        , String userCd
        , String termsId
        , String termsVersion
        , String agrYn
) {
    public static TermsAgrUpsertCommand of(String cmpnyCd, String userCd, String termsId, String termsVersion, String agrYn) {

        if (cmpnyCd == null || cmpnyCd.isBlank()
                || userCd == null || userCd.isBlank()
                || termsId == null || termsId.isBlank()
                || termsVersion == null || termsVersion.isBlank()
                || agrYn == null || agrYn.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new TermsAgrUpsertCommand(cmpnyCd, userCd, termsId, termsVersion, agrYn);
    }
}
