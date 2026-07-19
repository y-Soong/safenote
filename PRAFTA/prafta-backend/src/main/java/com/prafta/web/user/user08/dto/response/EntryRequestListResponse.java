package com.prafta.web.user.user08.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 입장 승인요청 목록 응답 (GET /webApi/user08/entry-request-lists).
 */
@Value
@Builder
public class EntryRequestListResponse {
    List<EntryRequestItem> entryRequestList;
    int totalCount;
}
