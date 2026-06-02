package com.prafta.app.nearmiss.nearmiss01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * A1 보고 등록 응답. 채번된 사건 ID 반환.
 */
@Getter
@Builder
public class ReportResponse {
    private String nearMissId;
}
