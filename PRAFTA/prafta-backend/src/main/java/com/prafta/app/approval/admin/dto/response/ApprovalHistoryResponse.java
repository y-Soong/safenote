package com.prafta.app.approval.admin.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 001-P2-B6: 승인 이력(A-5) 응답. plan §3-F 계약. 정렬 PROCESS_DATE DESC 고정.
 */
@Getter
@Builder
public class ApprovalHistoryResponse {

    private final List<HistoryItem> items;
    private final int totalCount;
    private final boolean hasMore;

    @Getter
    @Builder
    public static class HistoryItem {
        private final String reqId;
        private final String group;
        private final String reqType;
        private final String reqTypeNm;
        private final String requesterUserNm;
        private final String nodeNm;
        private final String targetYmd;
        private final String reqDate;
        private final String processDate;
        private final String reqStatus;
        private final String reqStatusNm;
        private final String processUserNm;
        private final String rejectReason;
    }
}
