package com.prafta.web.nearmiss.nearmiss01.dto.response;

import com.prafta.web.nearmiss.nearmiss01.result.IncidentResult;

import lombok.Builder;
import lombok.Getter;

/**
 * E2 사건 단건 상세 응답.
 */
@Getter
@Builder
public class IncidentInfoResponse {
    private IncidentResult incidentInfo;
}
