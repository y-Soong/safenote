package com.prafta.web.tbm.tbm02.result;

/**
 * 연동받은 교육(비개설사 전용 목록) 1행 — PRAFTA-SUBCON-T5 D2.
 *
 * <p>내 회사가 <b>유효하게 지정받은</b> 타사 세션의 헤더 최소 정보만 담는다. 재지정(체인 관리) 진입점을
 * 제공하는 것이 목적이며, 세션 상세/콘솔로는 들어갈 수 없다(개설사 전용 게이트 유지).
 *
 * <p><b>절대 포함하지 않는 것</b>(매퍼 SELECT 절에 아예 없다): 교육 본문(CONTENT_BODY), 교육자료,
 * 위험성평가, 참석자 명단/PII, 개설사 사업장코드·사업장명, 입실/종료 비밀번호, GPS 좌표.
 *
 * <p>{@code designatedByCmpnyNm} 은 <b>나를 지정한 회사</b>(내 SHARE 행의 DESIGNATED_BY_CMPNY_CD)의
 * 회사명이다. 개설사(HOST)가 아니다 — C 가 B 에게 재지정받았다면 B 만 보이고 A(개설사)는 보이지 않는다
 * (마스터 §1-3 인접 차수 가시성).
 *
 * <p>resultType record: SELECT 컬럼 순서 = 아래 필드 순서(위치기반 매핑)와 반드시 일치.
 */
public record SharedSessionResult(
	String sessionCd
	, String title
	, String eduTypeCd
	, String statusCd
	, String statusNm
	, String openedAt
	, String startedAt
	, String endedAt
	, String designatedByCmpnyNm	// 나를 지정한 회사명(개설사 아님)
	, String designatedDtime
	, int myAttendanceCount			// 내 회사 참석자 수(입실 기준)
){

}
