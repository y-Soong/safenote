package com.prafta.web.tbm.tbm04.result;

/** W-15 사용자별 이수 이력 행(세션 단위). */
public record UserAttendanceResult(
	String attendanceCd
	, String sessionCd
	, String sessionTitle
	, String siteCd
	, String siteNm
	, String sessionDate		// 세션 종료일(없으면 개설/등록일)
	, String entryAt
	, String exitAt
	, String completionStatusCd
	, String completionStatusNm
	, int riskCount				// 해당 세션의 연계 위험성평가 수
){
}
