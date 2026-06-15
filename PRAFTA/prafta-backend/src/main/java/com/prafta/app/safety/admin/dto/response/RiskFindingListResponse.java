package com.prafta.app.safety.admin.dto.response;

import java.util.List;

import com.prafta.app.safety.admin.result.RiskAssessmentResult;

import lombok.Builder;
import lombok.Getter;

/**
 * H3 위험성평가 목록 응답.
 */
@Getter
@Builder
public class RiskFindingListResponse {
    private List<RiskAssessmentResult> findings;
}
