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
	// PRAFTA-SUBCON-T5: 세션 개설 회사코드(내 회사와 다르면 타사 세션). 서비스가 이 값으로
	// 타사 세션 행의 사업장명을 비우고 개최 회사 라벨로 대체한다(plan D3). 응답에는 싣지 않는다.
	, String hostCmpnyCd
){
}
