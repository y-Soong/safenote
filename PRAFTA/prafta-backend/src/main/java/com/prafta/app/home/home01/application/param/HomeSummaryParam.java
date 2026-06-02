package com.prafta.app.home.home01.application.param;

import org.springframework.util.StringUtils;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-001: 메인화면 요약 조회 Param.
 * <p>식별값(cmpnyCd/siteCd/userCd)은 JWT 토큰에서만 도출한다(클라 입력 무시).
 * cross-site/cross-tenant IDOR 차단 — chkLst01 의 캐노니컬라이즈 패턴을 동일하게 따른다.
 */
public record HomeSummaryParam(
    String cmpnyCd
    , String siteCd
    , String userCd
    , TokenInfo tokenInfo
) {
    public static HomeSummaryParam from(TokenInfo tokenInfo) {

        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String siteCd = tokenInfo.gv_siteCd();
        String userCd = tokenInfo.gv_userCd();

        // 회사/사업장/사용자 식별값이 토큰에 없으면 명확한 에러
        if (!StringUtils.hasText(cmpnyCd)
                || !StringUtils.hasText(siteCd)
                || !StringUtils.hasText(userCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        return new HomeSummaryParam(cmpnyCd, siteCd, userCd, tokenInfo);
    }
}
