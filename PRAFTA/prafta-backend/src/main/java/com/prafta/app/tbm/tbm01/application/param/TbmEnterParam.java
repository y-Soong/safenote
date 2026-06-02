package com.prafta.app.tbm.tbm01.application.param;

import org.springframework.util.StringUtils;

import com.prafta.app.tbm.tbm01.dto.request.TbmEnterRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-004-C1: 입실 Param.
 * <p>요청 바디(sessionCd/entryPwd/lat/lon) + tokenInfo 를 service 단일 인자로 정리한다.
 * <p>USER_CD/CMPNY_CD 는 token 출처만 사용한다(IDOR 차단). lat/lon 은 GPS 검증/거리 기록용.
 */
public record TbmEnterParam(
    String sessionCd
    , String entryPwd
    , Double lat
    , Double lon
    , TokenInfo tokenInfo
) {
    public static TbmEnterParam from(TbmEnterRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        if (!StringUtils.hasText(request.getSessionCd()))
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (!StringUtils.hasText(request.getEntryPwd()))
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (!StringUtils.hasText(tokenInfo.gv_cmpnyCd()) || !StringUtils.hasText(tokenInfo.gv_userCd()))
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new TbmEnterParam(
            request.getSessionCd()
            , request.getEntryPwd()
            , request.getLat()
            , request.getLon()
            , tokenInfo
        );
    }
}
