package com.prafta.web.tbm.tbm04.result;

/**
 * W-12 기간 통계 요약. 조회 필터와 동일 스코프에서 집계.
 * 빈 데이터 시 모든 값 0(이수율 0).
 */
public record HistoryStatResult(
	int sessionCount		// 대상 TBM 횟수
	, int attendanceCount	// 참여 인원 합계
	, int completedCount	// 이수 합계
	, int notCompletedCount	// 미이수 합계
){
}
