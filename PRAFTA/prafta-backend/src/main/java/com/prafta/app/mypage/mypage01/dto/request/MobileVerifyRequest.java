package com.prafta.app.mypage.mypage01.dto.request;

import lombok.Data;

/**
 * prafta-app-010-03b: 휴대폰 변경 인증 검증 요청 (앱 전용, D4).
 */
@Data
public class MobileVerifyRequest {
    private String mblNo;
    private String verificationCode;
}
