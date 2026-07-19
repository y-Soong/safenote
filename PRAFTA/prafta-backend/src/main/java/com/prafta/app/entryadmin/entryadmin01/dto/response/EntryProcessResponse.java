package com.prafta.app.entryadmin.entryadmin01.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 앱 관리자 입장 승인/거부 처리 응답 (POST approve / reject).
 */
@Value
@Builder
public class EntryProcessResponse {
    int processedCount;
}
