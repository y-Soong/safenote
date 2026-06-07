package com.prafta.web.notice.notice01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 발행자 대상선택 트리 조회 파라미터(헤더 JWT 만 사용).
 * 발행자 스코프(소속 사업장/노드, 역할)는 모두 JWT 클레임에서 도출.
 */
public record NoticeScopeParam(
    String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
    , String gvNodeCd
){
    public static NoticeScopeParam from(TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new NoticeScopeParam(
            tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_nodeCd()
        );
    }
}
