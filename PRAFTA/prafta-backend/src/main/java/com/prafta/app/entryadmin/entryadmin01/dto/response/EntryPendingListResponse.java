package com.prafta.app.entryadmin.entryadmin01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 앱 관리자 입장 승인 대기 목록 응답 (GET /appApi/entryadmin01/pending-lists).
 */
@Value
@Builder
public class EntryPendingListResponse {
    List<EntryPendingItem> pendingList;
    int totalCount;
}
