package com.prafta.web.tbm.tbm04.result;

/**
 * tbm04-manager-sign — 주관자 서명 파일 식별 정보(서버 재조회용).
 *
 * <p>hostCmpnyCd: 개설사(주관자 서명 파일은 항상 개설사 스코프로 저장 — 파일 로드 회사키).
 * ownerYn: 요청자 회사 = 개설사 여부. attendedYn: 요청자 회사(자사) 참석(입실) 존재 여부 —
 * 공유 세션 참석사의 열람 인가 판단 재료(evidence-session-details 술어와 동치 정렬).
 * ⚠️ SELECT 컬럼 순서 = record 컴포넌트 순서(위치 매핑).
 */
public record ManagerSignInfoResult(
	String sessionCd
	, String hostCmpnyCd
	, String sessionSiteCd
	, String managerSignFileMgmtCd
	, String ownerYn           // 'Y'=자사 개설
	, String attendedYn        // 'Y'=자사 참석(입실) 존재
){
}
