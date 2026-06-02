package com.prafta.web.tbm.tbm04.result;

/** W-14 미이수 처리 게이트 검증용. 출결 row + 소속 세션 메타(스코프/개설자). */
public record AttendanceGuardResult(
	String attendanceCd
	, String sessionCd
	, String siteCd				// 소속 세션 사업장(스코프 격리)
	, String managerUserCd		// 소속 세션 개설자(개설자 본인 권한)
	, String completionStatusCd
){
}
