package com.prafta.web.leaverefusal.leaverefusal01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 노무수령거부 통지 발송 결과 (PRAFTA-COM-001 기능1).
 *
 * <p>요청 대상 건수와 신규 통지 적재 건수를 돌려준다. 멱등 특성상 이미 통지된 건은
 * noticedCount 에 포함되지 않을 수 있다(중복은 1건 유지).
 */
@Getter
@Builder
public class LeaveRefusalNoticeResponse {

    /** 요청 대상 총 건수 */
    private final int requestedCount;

    /** 신규로 통지(outbox+log) 적재된 건수 */
    private final int noticedCount;
}
