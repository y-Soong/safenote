package com.prafta.app.tbm.admin.dto.response;

import java.util.List;

import com.prafta.app.tbm.admin.result.AdminHistoryListResult;

import lombok.Builder;
import lombok.Getter;

/** R6 이력 리스트 응답(탭4) + 상단 통계. */
@Getter
@Builder
public class AdminHistoryListResponse {
    private List<AdminHistoryListResult> historyList;
    private int totalCount;
    private int page;
    private int pageSize;
    private StatSummary stat;

    /** 스코프 적용된 동일 조건 전체 집계(페이징 무관). avgCompletionRate=이수/참여*100. */
    @Getter
    @Builder
    public static class StatSummary {
        private int sessionCount;
        private int attendanceCount;
        private int completedCount;
        private int notCompletedCount;
        private double avgCompletionRate;
    }
}
