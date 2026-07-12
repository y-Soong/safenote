package com.prafta.web.tbm.tbm03.result;

/**
 * T7 드릴다운 세션 행(사용자 1명의 세션별 이수 이력).
 * 교육일 = DATE(IFNULL(S.ENDED_AT, S.INSERT_DATE)), 인정시간 = S.EDU_MINUTES(분).
 */
public record UserProgressDetailResult(
	String attendanceCd
	, String sessionCd
	, String sessionTitle
	, String sessionDate		// 세션 종료일(없으면 등록일)
	, Integer eduMinutes		// 인정시간(분, NULL 가능)
	, String entryAt
	, String exitAt
	, String completionStatusCd
	, String completionStatusNm
){
}
