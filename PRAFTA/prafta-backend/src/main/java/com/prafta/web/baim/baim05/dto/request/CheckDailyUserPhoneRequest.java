package com.prafta.web.baim.baim05.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 QR 발급 전 휴대폰 중복 사전확인 요청 (baim05-qr-phone-precheck).
 *
 * <p>[security Medium #3] 휴대폰 평문이 GET 쿼리스트링으로 전송되면 접근로그/브라우저
 * 히스토리/프록시에 잔존할 수 있어 POST 바디로 받는다(발급 EP 와 동일 방식).
 */
@Getter
@Setter
@NoArgsConstructor
public class CheckDailyUserPhoneRequest {
    private String siteCd;
    private String mblNo;
}
