package com.prafta.web.subcon.subcon01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon01.dto.request.RelationCreateRequest;

/**
 * 연동 관계 요청 생성 파라미터.
 *
 * <p>요청측 회사(gvCmpnyCd)/요청자(gvUserCd)는 JWT 클레임에서만 도출한다(클라 바디 불신).
 */
public record RelationCreateParam(
    String tgtCmpnyCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static RelationCreateParam from(RelationCreateRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new RelationCreateParam(
            request.getTgtCmpnyCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
