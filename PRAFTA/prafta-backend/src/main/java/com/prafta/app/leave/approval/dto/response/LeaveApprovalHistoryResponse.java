package com.prafta.app.leave.approval.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 사용자연차결재-01 (3-C 이력): 내가 처리(승인/반려)한 연차 결재 내역 응답.
 */
@Getter
@Builder
public class LeaveApprovalHistoryResponse {

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
        private final String leaveNm;
        private final String unitNm;
        private final BigDecimal leaveDays;
        private final String reqDate;
        /** 내 단계 처리 결과('02'승인/'03'반려). */
        private final String myDecision;
        private final String myDecisionNm;
        /** 내 단계 처리 일시. */
        private final String myProcessDate;
        /** 내 단계 코멘트(반려 사유 등). */
        private final String myComment;
        /** 요청 최종 상태(SYS033). */
        private final String reqStatus;
        private final String reqStatusNm;
    }
}
