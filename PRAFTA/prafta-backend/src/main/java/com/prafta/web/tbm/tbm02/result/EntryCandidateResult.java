package com.prafta.web.tbm.tbm02.result;

/**
 * 입실 후보 검색 결과 행(prafta-051-11).
 *
 * <p>일용직 PII 는 끝 4자리(mblNoLast4)만 노출하며, 정규직은 mblNoLast4=NULL.
 * alreadyEntered 는 DEL_YN='N' 입실 기록 존재 여부(기입실 표시/disabled 용).
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
){

}
