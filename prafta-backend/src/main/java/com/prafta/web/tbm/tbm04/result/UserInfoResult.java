package com.prafta.web.tbm.tbm04.result;

/**
 * W-15 대상 사용자 헤더 정보. 정규직/일용직 공용.
 * 일용직은 평문 휴대폰 금지 — MBL_NO_LAST4 만. 정규직은 소속 부서명.
 */
public record UserInfoResult(
	String userCd
	, String userTypeCd
	, String userNm
	, String siteCd
	, String siteNm
	, String deptNm				// 정규직 소속 부서명(일용직은 NULL)
	, String mblNoLast4			// 일용직 끝 4자리(정규직은 NULL)
){
}
