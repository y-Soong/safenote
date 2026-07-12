package com.prafta.web.tbm.tbm03.result;

/**
 * T7 드릴다운 대상 사용자 헤더 정보. 정규직/일용직 공용.
 * 휴대폰/이메일 미노출(PII 최소화). 일용직은 deptNm NULL.
 */
public record UserProgressUserResult(
	String userCd
	, String userTypeCd
	, String userNm
	, String employmentTypeNm	// 정규직 SYS041 라벨, 일용직 '일용직'
	, String siteCd				// 스코프 검증용(verifyScope)
	, String siteNm
	, String deptNm				// 정규직 소속 부서명(일용직 NULL)
){
}
