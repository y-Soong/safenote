package com.prafta.app.terms.terms01.application.param;

import com.prafta.app.terms.terms01.dto.request.OptionalTermsAgreeRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.terms.TermsErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 선택약관 토글 Param.
 *
 * <p>USER_CD 는 JWT 클레임(gv_userCd)에서만 도출(IDOR 차단).
 * <p>입력 검증:
 *   <ul>
 *     <li>tokenInfo/userCd 부재 → COMMON_400_003(진짜 인증 결함).</li>
 *     <li>termsId 누락/공백 또는 agrYn 화이트리스트(Y/N) 위반 → TERMS_400_001.</li>
 *   </ul>
 * <p>선택약관(REQUIRED_YN='N' AND USE_YN='Y') 여부 검증은 서비스 단계에서 수행한다(현재버전 resolve 동반).
 */
public record OptionalTermsAgreeParam(
        String cmpnyCd
        , String userCd
        , String termsId
        , String agrYn
) {
    public static OptionalTermsAgreeParam from(OptionalTermsAgreeRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String termsId = request.getTermsId();
        if (termsId == null || termsId.isBlank())
            throw new ApiException(TermsErrorCode.TERMS_400_001);

        String agrYn = request.getAgrYn();
        // 화이트리스트: 'Y' | 'N' 만 허용(그 외 위조값 차단).
        if (!"Y".equals(agrYn) && !"N".equals(agrYn))
            throw new ApiException(TermsErrorCode.TERMS_400_001);

        // cmpnyCd/userCd 모두 JWT 클레임에서만 도출(회사 스코프 약관 동의, IDOR 차단).
        return new OptionalTermsAgreeParam(
                tokenInfo.gv_cmpnyCd()
                , tokenInfo.gv_userCd()
                , termsId.trim()
                , agrYn
        );
    }
}
