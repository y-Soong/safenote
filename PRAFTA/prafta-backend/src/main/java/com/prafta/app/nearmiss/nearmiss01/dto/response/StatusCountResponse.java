package com.prafta.app.nearmiss.nearmiss01.dto.response;

import com.prafta.app.nearmiss.nearmiss01.result.StatusCountResult;

import lombok.Builder;
import lombok.Getter;

/**
 * A4 상태별 카운트 응답 (탭 배지: 접수/검토중/조치중/완료).
 */
@Getter
@Builder
public class StatusCountResponse {
    private StatusCountResult statusCount;
}
