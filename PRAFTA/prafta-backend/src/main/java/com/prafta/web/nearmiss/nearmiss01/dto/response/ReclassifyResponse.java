package com.prafta.web.nearmiss.nearmiss01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * E6 재분류 응답 (신규 생성된 사건 ID 반환).
 */
@Getter
@Builder
public class ReclassifyResponse {
    private String nearMissId;
}
