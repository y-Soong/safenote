package com.prafta.web.user.user06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user06.dto.request.BlacklistReleaseRequest;

/**
 * 블랙리스트 해제 파라미터.
 *
 * <p>회사 스코프(gvCmpnyCd)/수정자(gvUserCd)는 JWT 클레임에서만 도출한다(IDOR 방지).
 * blacklistId 만 클라가 지정하며, 해제는 회사 스코프 조건부 UPDATE 로만 동작한다.
 */
public record BlacklistReleaseParam(
    String blacklistId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static BlacklistReleaseParam from(BlacklistReleaseRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new BlacklistReleaseParam(
            request.getBlacklistId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
