package com.prafta.web.tbm.tbm04.application.query;

/**
 * W-14 미이수 처리 권한/스코프 게이트 검증용 경량 조회 쿼리.
 * attendanceCd → 출결 row + 소속 세션(SITE_CD/개설자) 조인 결과를 얻는다.
 */
public record AttendanceGuardQuery(
	String attendanceCd
	, String gvCmpnyCd
){
}
