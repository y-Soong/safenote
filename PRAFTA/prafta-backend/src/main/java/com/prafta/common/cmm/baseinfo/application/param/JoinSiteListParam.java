package com.prafta.common.cmm.baseinfo.application.param;

import org.springframework.util.StringUtils;

import com.prafta.common.cmm.baseinfo.dto.request.SiteInfoRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 회원가입(비로그인) 단계 사업장 조회 Param.
 *
 * <p>토큰이 없는 시점이므로 {@link SiteInfoParam} 과 달리 식별값을 클라이언트가 제공한 {@code cmpnyCd}
 * 에서 가져온다. 호출 endpoint 는 {@code @NoAuth} 로 면제되며, 회사 활성 검증은 동일 회원가입 흐름의
 * {@code GET /cmpny-infos} 호출에서 선행으로 수행되었음을 가정한다(클라이언트 흐름 가드).
 *
 * <p>본 Param 은 회원가입 단계 외에는 사용하지 않는다 (인증 후 화면은 토큰 기반 {@link SiteInfoParam} 사용).
 */
public record JoinSiteListParam(
        String cmpnyCd
        , String siteNo
        , String siteNm
) {
    public static JoinSiteListParam from(SiteInfoRequest request) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        String cmpnyCd = request.getCmpnyCd();
        if (!StringUtils.hasText(cmpnyCd))
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new JoinSiteListParam(
                cmpnyCd
                , request.getSiteNo()
                , request.getSiteNm()
        );
    }
}
