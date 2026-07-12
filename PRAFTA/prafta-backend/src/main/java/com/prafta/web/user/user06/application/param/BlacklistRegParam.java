package com.prafta.web.user.user06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user06.dto.request.BlacklistRegRequest;

/**
 * 블랙리스트 등록 파라미터.
 *
 * <p>회사 스코프(gvCmpnyCd)/등록자(gvUserCd)는 JWT 클레임에서만 도출한다(클라 바디 신뢰 금지).
 * mblNo 는 평문 입력이며, 서비스에서 정규화→HMAC/ENC/LAST4 로 변환만 한다(평문 저장 금지).
 */
public record BlacklistRegParam(
    String mblNo
    , String reason
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static BlacklistRegParam from(BlacklistRegRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new BlacklistRegParam(
            request.getMblNo()
            , request.getReason()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
