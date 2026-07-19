package com.prafta.app.terms.terms01.application.param;

import com.prafta.app.terms.terms01.dto.request.SubconConsentRespondRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.terms.TermsErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 연동 회사 제3자 제공 동의(006) 응답 Param — PRAFTA-SUBCON-T4-03.
 *
 * <p>cmpnyCd/userCd 는 JWT 클레임에서만 도출(IDOR 차단).
 * <p>agrYn 화이트리스트('Y'|'N') 위반 → TERMS_400_001.
 *    (★ COMMON_400_003/COMMON_400_600 은 앱 인터셉터가 토큰 오류로 간주해 강제 로그아웃시키므로
 *      입력 검증 실패에는 사용하지 않는다.)
 * <p>termsId/termsVersion 은 Param 에 담지 않는다 — 서비스가 서버 상수/현재버전으로 확정한다.
 */
public record SubconConsentRespondParam(
        String cmpnyCd
        , String userCd
        , String agrYn
) {
    public static SubconConsentRespondParam from(SubconConsentRespondRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String agrYn = request.getAgrYn();
        // 화이트리스트: 'Y' | 'N' 만 허용(미동의도 정상 응답이므로 'N' 저장 대상이다).
        if (!"Y".equals(agrYn) && !"N".equals(agrYn))
            throw new ApiException(TermsErrorCode.TERMS_400_001);

        return new SubconConsentRespondParam(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd(), agrYn);
    }
}
