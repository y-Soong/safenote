package com.prafta.web.tbm.tbm04.result;

/** W-12 이력 목록 행. 출결 집계는 출결 테이블 LEFT JOIN/서브쿼리(빈 데이터 시 0). */
public record HistorySessionListResult(
	String sessionCd
	, String siteCd
	, String siteNm
	, String title
	, String statusCd
	, String statusNm
	, String managerUserCd
	, String managerUserNm
	, String openedAt
	, String startedAt
	, String endedAt			// 종료일(COMPLETED 기준 표시)
	, String insertDate
	, int riskCount				// 연계 위험성평가 수(0이면 화면 경고)
	, int attendanceCount		// 참여 인원(출결 수)
	, int completedCount		// 이수 수
	, int notCompletedCount		// 미이수 수(빨강 강조)
){
}
