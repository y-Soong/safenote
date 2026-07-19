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
	// PRAFTA-SUBCON-T5 F4: 세션 개설 회사코드(내 회사와 다르면 타사 세션). 서비스가 개최 회사 라벨
	// (hostCmpnyNm)로 변환하며 회사코드 자체는 응답에 싣지 않는다(D3 정합).
	, String hostCmpnyCd
){
}
