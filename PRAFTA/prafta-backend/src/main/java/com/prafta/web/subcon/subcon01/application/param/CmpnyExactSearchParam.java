package com.prafta.web.subcon.subcon01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon01.dto.request.CmpnyExactSearchRequest;

/**
 * 회사 정확일치 조회 파라미터.
 *
 * <p>자기 회사 스코프(gvCmpnyCd)는 JWT 클레임에서만 도출한다(클라 바디 신뢰 금지).
 */
public record CmpnyExactSearchParam(
    String cmpnyCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static CmpnyExactSearchParam from(CmpnyExactSearchRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new CmpnyExactSearchParam(
            request.getCmpnyCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
