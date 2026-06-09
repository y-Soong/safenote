package com.prafta.app.leave.approval.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 사용자연차결재-01 (3-A 대기): 결재 대기 목록 응답. group='LEAVE' 고정(단일 유형, counts 불필요).
 */
@Getter
@Builder
public class LeaveApprovalPendingResponse {

    private final List<PendingItem> items;
    private final int totalCount;
    private final boolean hasMore;

    @Getter
    @Builder
    public static class PendingItem {
        private final String reqId;
        private final Integer approvalStep;
        private final String group;
        private final String reqType;
        private final String reqTypeNm;
        private final String requesterUserNm;
        private final String requesterUserCd;
        private final String nodeNm;
        private final String targetYmd;
        private final String leaveNm;
        private final String unitNm;
        private final BigDecimal leaveDays;
        private final Integer leaveMinutes;
        private final String startTime;
        private final String endTime;
        private final List<String> summaryLines;
        private final String reqDate;
        /** 요청자 == 결재자(본인) 여부(§7.3). */
        private final String selfYn;
    }
}
