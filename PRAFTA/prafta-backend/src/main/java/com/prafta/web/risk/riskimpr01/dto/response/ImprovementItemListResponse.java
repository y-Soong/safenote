package com.prafta.web.risk.riskimpr01.dto.response;

import java.util.List;

import com.prafta.web.risk.riskimpr01.result.ImprovementItemResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 개선항목 목록 응답.
 */
@Getter
@Builder
public class ImprovementItemListResponse {
    private List<ImprovementItemResult> improvementItemList;
}
