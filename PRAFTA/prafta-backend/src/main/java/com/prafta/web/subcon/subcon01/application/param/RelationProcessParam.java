package com.prafta.web.subcon.subcon01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon01.dto.request.RelationProcessRequest;

/**
 * 연동 관계 상태 전이(수락/거부/취소/해지) 파라미터.
 *
 * <p>행위자 소속 회사(gvCmpnyCd)/행위자(gvUserCd)는 JWT 클레임에서만 도출한다.
 * 당사자 조건(수락·거부=TGT, 취소=REQ, 해지=양측)은 매퍼 조건부 UPDATE 로 강제한다.
 */
public record RelationProcessParam(
    Long relationId
    , String comment
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static RelationProcessParam from(RelationProcessRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new RelationProcessParam(
            request.getRelationId()
            , request.getComment()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
