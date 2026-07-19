package com.prafta.web.tbm.tbm02.result;

/**
 * 입실 후보 검색 결과 행(prafta-051-11 + PRAFTA-SUBCON-T5).
 *
 * <p>일용직 PII 는 끝 4자리(mblNoLast4)만 노출하며, 정규직은 mblNoLast4=NULL.
 * alreadyEntered 는 DEL_YN='N' 입실 기록 존재 여부(기입실 표시/disabled 용).
 *
 * <p><b>T5</b>: {@code cmpnyCd}(후보가 실제로 속한 회사)는 <b>응답에 싣지 않는다</b>. 서비스가 이 값으로
 * 불투명 핸들(M1)을 발급하고, 타사 행이면 사업장(siteCd/siteNm)을 null 로 접는다(M2 — 사업장명으로
 * 2차 회사가 식별되는 것을 막는다, 마스터 §1-3).
 *
 * <p>resultType record: SELECT 컬럼 순서 = 아래 필드 순서(위치기반 매핑)와 반드시 일치.
 */
public record EntryCandidateResult(
	String userCd
	, String userId
	, String userNm
	, String siteCd
	, String siteNm
	, String mblNoLast4
	, boolean alreadyEntered
	// PRAFTA-SUBCON-T5: 후보가 실제로 속한 회사코드(핸들 발급/사업장 접기 판정용. 응답 비노출)
	, String cmpnyCd
){

}
