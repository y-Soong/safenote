package com.prafta.web.user.user06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user06.dto.request.BlacklistListRequest;

/**
 * 블랙리스트 목록 조회 파라미터.
 *
 * <p>회사 스코프(gvCmpnyCd)는 JWT 클레임에서만 도출한다(클라 바디 신뢰 금지).
 * mblNo 는 평문 입력이며, 서비스에서 정규화→HMAC/LAST4 파생값으로 변환해 쿼리에 전달한다.
 */
public record BlacklistListParam(
    String mblNo
    , String useYn
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static BlacklistListParam from(BlacklistListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new BlacklistListParam(
            request.getMblNo()
            , request.getUseYn()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
