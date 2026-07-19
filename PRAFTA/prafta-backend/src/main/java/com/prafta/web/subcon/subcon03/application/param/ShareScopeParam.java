package com.prafta.web.subcon.subcon03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 데이터 공유 목록/수신 스냅샷 목록 조회 파라미터(검색조건 없음 — 회사 스코프만).
 *
 * <p>회사 스코프(gvCmpnyCd)는 JWT 클레임에서만 도출한다(클라 바디 회사코드 불신).
 */
public record ShareScopeParam(
    String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ShareScopeParam from(TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ShareScopeParam(
            tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
