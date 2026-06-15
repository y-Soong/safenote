package com.prafta.app.safety.admin.dto.response;

import com.prafta.app.safety.admin.result.RiskAssessmentResult;

import lombok.Builder;
import lombok.Getter;

/**
 * H4 위험성평가 상세 단건 응답.
 */
@Getter
@Builder
public class RiskDetailResponse {
    private RiskAssessmentResult detail;
}
