package com.prafta.app.nearmiss.nearmiss01.dto.response;

import java.util.List;

import com.prafta.app.nearmiss.nearmiss01.result.IncidentResult;

import lombok.Builder;
import lombok.Getter;

/**
 * A2 내 보고 목록 / A3 사업장 사건 목록 응답.
 */
@Getter
@Builder
public class IncidentListResponse {
    private List<IncidentResult> incidentResultList;
}
