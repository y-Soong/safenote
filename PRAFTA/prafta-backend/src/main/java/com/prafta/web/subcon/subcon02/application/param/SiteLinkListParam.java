package com.prafta.web.subcon.subcon02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 사업장 연동 링크 목록/제안 후보 조회 파라미터(검색조건 없음 — 회사 당사자 스코프만).
 *
 * <p>회사 스코프(gvCmpnyCd)는 JWT 클레임에서만 도출한다.
 */
public record SiteLinkListParam(
    String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static SiteLinkListParam from(TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new SiteLinkListParam(
            tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
