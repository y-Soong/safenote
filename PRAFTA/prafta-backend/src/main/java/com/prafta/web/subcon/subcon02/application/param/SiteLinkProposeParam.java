package com.prafta.web.subcon.subcon02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon02.dto.request.SiteLinkProposeRequest;

/**
 * 사업장 연동 제안 파라미터(PRAFTA-SUBCON-T2 §5-3).
 *
 * <p>제안측 회사(gvCmpnyCd)/제안자(gvUserCd)는 JWT 클레임에서만 도출한다.
 * 사업장 소유·관계 ACCEPTED·루프·중복 가드는 서비스에서 서버 강제한다.
 */
public record SiteLinkProposeParam(
    String tgtCmpnyCd
    , String siteCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static SiteLinkProposeParam from(SiteLinkProposeRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new SiteLinkProposeParam(
            request.getTgtCmpnyCd()
            , request.getSiteCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
