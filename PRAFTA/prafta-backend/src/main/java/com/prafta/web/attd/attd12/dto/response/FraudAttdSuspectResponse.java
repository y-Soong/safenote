package com.prafta.web.attd.attd12.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-com-003 C6 - 부정 출퇴근 의심 조회 응답.
 */
@Getter
@Builder
public class FraudAttdSuspectResponse {
    private final List<FraudSuspectRow> fraudSuspectRowList;
}
