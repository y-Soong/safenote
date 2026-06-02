package com.prafta.app.tbm.tbm01.application.param;

import org.springframework.util.StringUtils;

import com.prafta.app.tbm.tbm01.dto.request.TbmEntryContextRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-004-C3: 입실 컨텍스트 조회 Param.
 * <p>sessionCd(요청) + tokenInfo 를 service 단일 인자로 정리한다.
 * <p>USER_CD/CMPNY_CD 는 token 출처만 사용한다(IDOR 차단).
 */
public record TbmEntryContextParam(
    String sessionCd
    , TokenInfo tokenInfo
) {
    public static TbmEntryContextParam from(TbmEntryContextRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        if (!StringUtils.hasText(request.getSessionCd()))
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (!StringUtils.hasText(tokenInfo.gv_cmpnyCd()) || !StringUtils.hasText(tokenInfo.gv_userCd()))
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new TbmEntryContextParam(request.getSessionCd(), tokenInfo);
    }
}
