package com.prafta.web.nearmiss.nearmiss01.dto.response;

import java.util.List;

import com.prafta.web.nearmiss.nearmiss01.result.IncidentResult;

import lombok.Builder;
import lombok.Getter;

/**
 * E1 사건 목록 응답.
 */
@Getter
@Builder
public class IncidentListResponse {
    private List<IncidentResult> incidentResultList;
}
