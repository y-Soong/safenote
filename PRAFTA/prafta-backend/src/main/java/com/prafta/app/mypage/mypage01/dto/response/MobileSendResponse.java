package com.prafta.app.mypage.mypage01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-010-03a: 휴대폰 변경 인증번호 발송 응답 (앱 전용, D4).
 */
@Getter
@Builder
public class MobileSendResponse {

    /** 인증번호 만료까지 남은 초 (plan §4: 180). */
    private final int expiresInSeconds;
}
