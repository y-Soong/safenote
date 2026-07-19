package com.prafta.web.tbm.tbmai02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbmai02.dto.request.TbmAi02Request;

/**
 * TBM AI 교육안 생성 파라미터.
 * 식별자(cmpnyCd/userCd/authCd)는 JWT 클레임에서만 도출한다(IDOR 차단).
 * 세션 식별자(sessionCd)는 바디에서 받아 서비스에서 회사 소유·존재를 검증한다.
 */
public record TbmAi02Param(
    String sessionCd
    , String adminContentText
    , Integer targetChars
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {
    public static TbmAi02Param from(TbmAi02Request request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new TbmAi02Param(
            request.getSessionCd()
            , request.getAdminContentText()
            , request.getTargetChars()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
