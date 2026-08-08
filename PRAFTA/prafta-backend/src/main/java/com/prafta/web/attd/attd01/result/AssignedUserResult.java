package com.prafta.web.attd.attd01.result;

/**
 * F-12-2: 근무타입(SCH_CD) 배정현황 조회 1행.
 *
 * <p>{@code SchAssignedUsersPop.vue} 그리드에 표시할 사용자별 배정 요약 —
 * 최초 배정일, 최근 배정일, 배정 일수(guardScheduleDeactivate 와 동일 기준으로 집계).
 */
public record AssignedUserResult(
	String userNm
	, String nodeNm
	, String firstWorkYmd
	, String lastWorkYmd
	, Integer assignedDayCount
){

}
