package com.prafta.web.user.user08.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 입장 승인/거부 처리 응답 (POST entry-approve / entry-reject).
 */
@Value
@Builder
public class EntryProcessResponse {
    int processedCount;
}
