package com.prafta.web.tbm.tbm02.result;

/**
 * 교육준비(OPENED) 단계 입실자 명단 행(prafta-051-12).
 *
 * <p>정규직/일용직 통합(USER_TYPE 분기 조인). 일용직 PII 는 끝 4자리만. exited 는 EXIT_AT 존재
 * 여부(이미 종료 처리됨 표시용), entryDistanceM 은 반경초과 배지 산출용.
 *
 * <p>resultType record: SELECT 컬럼 순서 = 아래 필드 순서(위치기반 매핑)와 반드시 일치.
 */
public record SessionAttendanceResult(
	String attendanceCd
	, String userTypeCd
	, String userNm
	, String mblNoLast4
	, String entryTypeCd
	, String entryAt
	, Integer entryDistanceM
	, boolean exited
){

}
