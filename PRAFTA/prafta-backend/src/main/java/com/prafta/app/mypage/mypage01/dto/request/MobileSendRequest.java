package com.prafta.app.mypage.mypage01.dto.request;

import lombok.Data;

/**
 * prafta-app-010-03a: 휴대폰 변경 인증번호 발송 요청 (앱 전용, D4).
 */
@Data
public class MobileSendRequest {
    private String mblNo;
}
