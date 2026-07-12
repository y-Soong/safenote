package com.prafta.web.tbm.tbm02.result;

public record SessionListResult(
	String sessionCd
	, String siteCd
	, String siteNm
	, String title
	, String statusCd
	, String statusNm
	, String eduTypeCd
	, Integer eduMinutes		// 교육 인정시간(분, 1~60). 미설정 시 null
	, String managerUserCd
	, String managerUserNm
	, String openedAt
	, String startedAt
	, String endedAt
	, String insertDate
	, int riskCount				// 연계 위험성평가 수(0이면 화면 경고)
	, int attendanceCount		// 출결 수(출결 테이블 미사용 단계에서는 0)
	, int completedCount		// 이수 수
	, int notCompletedCount		// 미이수 수
){

}
