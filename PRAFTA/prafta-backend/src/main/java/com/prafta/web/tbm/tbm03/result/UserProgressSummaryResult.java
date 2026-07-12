package com.prafta.web.tbm.tbm03.result;

/**
 * T7 드릴다운 요약 통계. 기간/이수상태 필터와 동일 스코프로 집계.
 * 누적 교육시간은 이수(COMPLETED) 세션 EDU_MINUTES 합(D-4). SUM 0건은 IFNULL 0 보정.
 */
public record UserProgressSummaryResult(
	int totalEduMinutes			// 누적 교육시간(이수 세션 EDU_MINUTES 합)
	, int completedCount		// 수료 세션수
	, int notCompletedCount		// 미이수 세션수(입실했으나 미완료)
){
}
