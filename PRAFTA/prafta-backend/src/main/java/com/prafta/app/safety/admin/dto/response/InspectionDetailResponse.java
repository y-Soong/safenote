package com.prafta.app.safety.admin.dto.response;

import java.util.List;

import com.prafta.app.safety.admin.result.InspectionAnswerResult;

import lombok.Builder;
import lombok.Getter;

/**
 * H2 순회점검 상세 응답(일자별 답변 + 불량 사진/비고). 불량(inspectAnswerType='X') 우선 정렬.
 */
@Getter
@Builder
public class InspectionDetailResponse {
    private List<InspectionAnswerResult> answers;
}
