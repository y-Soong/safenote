package com.prafta.app.user.user01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 앱 소속이동 안내 확인(ack) 요청 — PRAFTA-WEB_001-5(Terminal E).
 *
 * <p>대상자는 토큰(JWT userCd)으로만 결정한다(IDOR 방지). 본 바디는 확인할 예약 ID 만 담는다.
 * 웹(webApi) 컨트롤러의 TransferNoticeAckRequest 와 동일 계약이나, 앱 모듈 자기완결성을 위해 별도 선언한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AppTransferNoticeAckRequest {
    private String reservationId;
}
