package com.prafta.web.tbm.tbm04.result;

/**
 * W-15 사용자별 이수 통계. 이수율 = 이수/총참여(서버 산출). 빈 데이터 시 0.
 * 평균 참여시간(분)은 입실~종료가 모두 있는 출결 기준 평균.
 */
public record UserAttendanceSummaryResult(
	int totalCount			// 총 참여
	, int completedCount	// 이수
	, int notCompletedCount	// 미이수
	, Integer avgDurationMin	// 평균 참여시간(분, 산출 불가 시 NULL)
){
}
