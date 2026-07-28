package com.prafta.common.cmm.consent.application.param;

import com.prafta.common.cmm.consent.dto.request.ConsentOptionalTermsAgreeRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.terms.TermsErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 선택약관 토글 Param(웹 경로) — 앱 OptionalTermsAgreeParam 미러.
 *
 * <p>cmpnyCd/userCd 는 JWT 클레임에서만 도출(IDOR 차단).
 * <p>입력 검증: termsId 누락/공백 또는 agrYn 화이트리스트(Y/N) 위반 → TERMS_400_001.
 *    (COMMON_400_003/COMMON_400_600 은 프론트 인터셉터가 토큰 오류로 간주해 강제 로그아웃시키므로
 *     단순 입력 검증 실패에는 쓰지 않는다.)
 * <p>선택약관 여부/현재버전 검증은 서비스 단계에서 수행한다.
 */
public record ConsentToggleParam(
        String cmpnyCd
        , String userCd
        , String termsId
        , String agrYn
) {
    public static ConsentToggleParam from(ConsentOptionalTermsAgreeRequest request, TokenInfo tokenInfo) {

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

        return new ConsentToggleParam(
                tokenInfo.gv_cmpnyCd()
                , tokenInfo.gv_userCd()
                , termsId.trim()
                , agrYn
        );
    }
}
