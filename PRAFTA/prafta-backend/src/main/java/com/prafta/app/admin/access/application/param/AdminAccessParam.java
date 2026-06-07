package com.prafta.app.admin.access.application.param;

import org.springframework.util.StringUtils;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 001-P1-B1: 진입판정(access-context) 입력 Param.
 *
 * <p>식별자(cmpnyCd/userCd/authCd/siteCd)는 token 에서만 도출한다(IDOR 차단). 클라이언트는 어떤 식별자도
 * path/query/body 로 보낼 수 없다. 유일한 예외는 현장전환 재조회용 선택 {@code selectedSiteCd}(쿼리)이며,
 * 서버가 USE_YN='Y' 로 검증한 뒤에만 채택한다(D5, assertSiteAccess).
 *
 * @param selectedSiteCd 현장전환으로 선택한 사업장코드(선택). 미지정이면 토큰 gv_siteCd 를 사용.
 */
public record AdminAccessParam(
    TokenInfo tokenInfo
    , String selectedSiteCd
) {
    public static AdminAccessParam from(String selectedSiteCd, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        if (!StringUtils.hasText(tokenInfo.gv_cmpnyCd())
                || !StringUtils.hasText(tokenInfo.gv_userCd()))
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // 빈 문자열은 미지정으로 정규화(토큰 사업장 사용).
        String normalizedSiteCd = StringUtils.hasText(selectedSiteCd) ? selectedSiteCd.trim() : null;

        return new AdminAccessParam(tokenInfo, normalizedSiteCd);
    }
}
