package com.prafta.web.subcon.subcon01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon01.dto.request.RelationHistRequest;

/**
 * 연동 관계 이력/해지 영향 요약 조회 파라미터.
 *
 * <p>당사자 검증(gvCmpnyCd 가 REQ 또는 TGT)은 서비스/매퍼에서 강제한다.
 */
public record RelationHistParam(
    Long relationId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static RelationHistParam from(RelationHistRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new RelationHistParam(
            request.getRelationId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
