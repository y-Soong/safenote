package com.prafta.app.safety.admin.dto.response;

import java.util.List;

import com.prafta.app.safety.admin.result.InspectionPointResult;

import lombok.Builder;
import lombok.Getter;

/**
 * H1 순회점검 결과 리스트 응답(포인트별 점검일수/불량수).
 */
@Getter
@Builder
public class InspectionListResponse {
    private List<InspectionPointResult> points;
}
