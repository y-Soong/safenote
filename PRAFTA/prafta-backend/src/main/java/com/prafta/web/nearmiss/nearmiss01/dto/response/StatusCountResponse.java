package com.prafta.web.nearmiss.nearmiss01.dto.response;

import com.prafta.web.nearmiss.nearmiss01.result.StatusCountResult;

import lombok.Builder;
import lombok.Getter;

/**
 * E3 상태별 카운트 응답 (탭 배지: 접수/검토중/조치중/완료).
 */
@Getter
@Builder
public class StatusCountResponse {
    private StatusCountResult statusCount;
}
