package com.prafta.app.mypage.mypage01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-010-03b: 휴대폰 변경 인증 검증 응답 (앱 전용, D4).
 *
 * <p>로그인 토큰은 발급하지 않는다. 검증 성공 시 단발성 verificationToken(scope=PHONE_CHANGE_AUTH, 5분)만
 * 반환하며, 이 토큰은 프로필 저장(010-02)의 휴대폰 변경 검증에 사용된다.
 */
@Getter
@Builder
public class MobileVerifyResponse {

    private final boolean verified;
    private final String verificationToken;
}
