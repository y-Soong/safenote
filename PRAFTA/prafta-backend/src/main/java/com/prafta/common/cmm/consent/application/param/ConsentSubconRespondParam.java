package com.prafta.common.cmm.consent.application.param;

import com.prafta.common.cmm.consent.dto.request.ConsentSubconRespondRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.terms.TermsErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 연동 회사 제3자 제공 동의(006) 응답 Param(웹 경로) — 앱 SubconConsentRespondParam 미러.
 *
 * <p>cmpnyCd/userCd 는 JWT 클레임에서만 도출(IDOR 차단).
 * <p>agrYn 화이트리스트('Y'|'N') 위반 → TERMS_400_001.
 * <p>termsId/termsVersion 은 담지 않는다 — 서비스가 서버 상수/현재버전으로 확정한다.
 */
public record ConsentSubconRespondParam(
        String cmpnyCd
        , String userCd
        , String agrYn
) {
    public static ConsentSubconRespondParam from(ConsentSubconRespondRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String agrYn = request.getAgrYn();
        // 화이트리스트: 'Y' | 'N' 만 허용(미동의도 정상 응답이므로 'N' 저장 대상이다).
        if (!"Y".equals(agrYn) && !"N".equals(agrYn))
            throw new ApiException(TermsErrorCode.TERMS_400_001);

        return new ConsentSubconRespondParam(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd(), agrYn);
    }
}
