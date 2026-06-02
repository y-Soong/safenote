package com.prafta.web.tbm.tbm04.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm04.result.HistorySessionListResult;

import lombok.Builder;
import lombok.Getter;

/** W-12 이력 목록 응답(목록 + 페이징 + 기간 통계). */
@Getter
@Builder
public class HistorySessionListResponse {
	private List<HistorySessionListResult> historyList;
	private int totalCount;
	private int page;
	private int pageSize;

	/** 기간 통계 요약. */
	private StatSummary stat;

	@Getter
	@Builder
	public static class StatSummary {
		private int sessionCount;		// 기간 TBM 횟수
		private int attendanceCount;	// 참여 인원 합계
		private int completedCount;		// 이수 합계
		private int notCompletedCount;	// 미이수 합계
		private double avgCompletionRate;	// 평균 이수율(%) — 이수/참여 * 100, 빈 데이터 0
	}
}
