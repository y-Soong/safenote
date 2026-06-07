package com.prafta.app.tbm.tbm01.application.param;

import org.springframework.util.StringUtils;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-tbm: 세션 단건 상세/액션(A4~A10) 공용 Param.
 * <p>sessionCd 는 path 변수에서, 식별자(CMPNY/SITE/USER)는 token 에서만 도출한다(IDOR 차단).
 * <p>조회/액션 모두 동일 입력(sessionCd + token)이므로 단일 Param 으로 통일한다.
 */
public record TbmSessionDetailParam(
    String sessionCd
    , TokenInfo tokenInfo
) {
    public static TbmSessionDetailParam from(String sessionCd, TokenInfo tokenInfo) {

        if (!StringUtils.hasText(sessionCd))
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        if (!StringUtils.hasText(tokenInfo.gv_cmpnyCd())
                || !StringUtils.hasText(tokenInfo.gv_siteCd())
                || !StringUtils.hasText(tokenInfo.gv_userCd()))
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new TbmSessionDetailParam(sessionCd, tokenInfo);
    }
}
