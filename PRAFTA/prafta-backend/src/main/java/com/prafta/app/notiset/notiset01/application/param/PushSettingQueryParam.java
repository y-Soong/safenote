package com.prafta.app.notiset.notiset01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 푸시 설정 조회(GET) Param (PRAFTA-APP-021-1).
 *
 * <p>cmpnyCd/userCd/authCd 는 JWT 클레임에서만 도출한다(IDOR 차단). 본문 식별자 미사용.
 * authCd 는 isAdmin 판정(전사역할 master/hr/safe)에 사용한다.
 */
public record PushSettingQueryParam(
    String cmpnyCd
    , String userCd
    , String authCd
){
    public static PushSettingQueryParam from(TokenInfo tokenInfo) {
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isBlank()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank()) {
            // 진짜 인증 결함만 003(앱 인터셉터 강제 로그아웃 트리거).
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return new PushSettingQueryParam(
            tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
