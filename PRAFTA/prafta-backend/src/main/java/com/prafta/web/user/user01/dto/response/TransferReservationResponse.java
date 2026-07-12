package com.prafta.web.user.user01.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 소속이동 예약 등록 응답(채번된 reservationId 반환) — PRAFTA-WEB_001-1.
 */
@Value
@Builder
public class TransferReservationResponse {
    String reservationId;
}
