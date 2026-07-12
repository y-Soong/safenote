package com.prafta.web.user.user01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 소속이동 안내 확인(ack) 요청 — PRAFTA-WEB_001-3.
 *
 * <p>대상자는 토큰(JWT userCd)으로만 결정한다(IDOR 방지). 본 바디는 확인할 예약 ID 만 담는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class TransferNoticeAckRequest {
    private String reservationId;
}
