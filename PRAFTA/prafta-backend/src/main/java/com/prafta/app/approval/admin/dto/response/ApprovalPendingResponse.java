package com.prafta.app.approval.admin.dto.response;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * 001-P2-B2: 승인 대기 리스트(A-1) 응답. plan §3-B 계약.
 */
@Getter
@Builder
public class ApprovalPendingResponse {

    private final List<PendingItem> items;
    /** 그룹별 대기 건수: ALL/CORRECTION/OVERTIME/LEAVE. */
    private final Map<String, Integer> counts;
    private final int totalCount;
    private final boolean hasMore;

    @Getter
    @Builder
    public static class PendingItem {
        private final String reqId;
        private final String group;
        private final String reqType;
        private final String reqTypeNm;
        private final String requesterUserNm;
        private final String requesterUserCd;
        private final String nodeNm;
        private final String targetYmd;
        private final List<String> summaryLines;
        private final String reqDate;
        /** A1(마감 기준일 소스) 미확정 — v1 보류(null). */
        private final Integer deadlineDday;
        private final String deadlineLevel;
        private final String selfYn;
        /** 선점 잠금 미구현(A-4 제외) — 항상 false. */
        private final boolean lockedYn;
        private final String lockedByNm;
        /** 연차만 결재 단계(근태보정/초과는 null). */
        private final Integer approvalStep;
    }
}
