package com.prafta.web.subcon.subcon02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon02.dto.request.SiteLinkProcessRequest;

/**
 * 사업장 연동 링크 상태 전이(수락/거부/취소/해지) 파라미터.
 *
 * <p>행위자 소속 회사(gvCmpnyCd)/행위자(gvUserCd)는 JWT 클레임에서만 도출한다.
 * 당사자 조건(수락·거부=DST, 취소=SRC, 해지=양측)은 매퍼 조건부 UPDATE 로 강제한다.
 */
public record SiteLinkProcessParam(
    Long linkId
    , String comment
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static SiteLinkProcessParam from(SiteLinkProcessRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new SiteLinkProcessParam(
            request.getLinkId()
            , request.getComment()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
