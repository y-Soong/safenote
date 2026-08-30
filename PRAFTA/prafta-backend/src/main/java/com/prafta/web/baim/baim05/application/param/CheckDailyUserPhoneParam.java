package com.prafta.web.baim.baim05.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 관리자 QR 발급 전 휴대폰 중복 사전확인 파라미터 (baim05-qr-phone-precheck).
 *
 * <p>JWT 클레임 도출값(gvCmpnyCd/gvUserCd/gvAuthCd)만 신뢰한다(클라 바디 신뢰 금지).
 * 휴대폰번호는 존재/형식만 여기서 확인하고, 정규화/HMAC 파생은 서비스에서 발급 경로와 동일하게 수행한다.
 */
public record CheckDailyUserPhoneParam(
    String siteCd
    , String mblNo
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static CheckDailyUserPhoneParam from(String siteCd, String mblNo, TokenInfo tokenInfo) {

        if (siteCd == null || siteCd.isBlank() || siteCd.length() > 50
            || mblNo == null || mblNo.isBlank() || mblNo.length() > 20) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new CheckDailyUserPhoneParam(
            siteCd
            , mblNo
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
