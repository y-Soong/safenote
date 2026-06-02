package com.prafta.app.nearmiss.nearmiss01.dto.response;

import com.prafta.app.nearmiss.nearmiss01.result.IncidentResult;

import lombok.Builder;
import lombok.Getter;

/**
 * A5 사건 단건 상세 응답.
 */
@Getter
@Builder
public class IncidentInfoResponse {
    private IncidentResult incidentInfo;
}
