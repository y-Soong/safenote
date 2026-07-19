package com.prafta.web.subcon.subcon01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 연동 관계 목록 조회 파라미터(검색조건 없음 — 회사 당사자 스코프만).
 *
 * <p>회사 스코프(gvCmpnyCd)는 JWT 클레임에서만 도출한다.
 */
public record RelationListParam(
    String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static RelationListParam from(TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new RelationListParam(
            tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
