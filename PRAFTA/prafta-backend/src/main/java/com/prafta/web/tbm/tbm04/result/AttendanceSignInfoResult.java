package com.prafta.web.tbm.tbm04.result;

/**
 * W-13 확장 — 출결 서명 파일 식별 정보(서버 재조회용).
 *
 * <p>attendeeCmpnyCd: 참석자 회사(타사 참석자 가능 — 서명 파일은 참석자 회사 스코프로 저장되어
 * 파일 로드 시 이 값을 회사키로 쓴다). sessionSiteCd: 세션 사업장(스코프 격리 검증용).
 */
public record AttendanceSignInfoResult(
	String attendanceCd
	, String attendeeCmpnyCd
	, String sessionSiteCd
	, String entrySignFileMgmtCd
	, String exitSignFileMgmtCd
){
}
