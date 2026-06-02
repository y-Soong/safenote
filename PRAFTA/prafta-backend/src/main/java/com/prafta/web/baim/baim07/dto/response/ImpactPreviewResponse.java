package com.prafta.web.baim.baim07.dto.response;

import com.prafta.common.cmm.leave.vo.ImpactSummaryVO;

import lombok.Builder;
import lombok.Value;

/**
 * 영향 분석 미리보기 응답 (저장 없음).
 */
@Value
@Builder
public class ImpactPreviewResponse {

    ImpactSummaryVO impactSummary;
}
