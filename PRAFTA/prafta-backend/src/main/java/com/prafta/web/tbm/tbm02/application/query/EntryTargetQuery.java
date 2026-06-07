package com.prafta.web.tbm.tbm02.application.query;

/**
 * 대리입실 대상 사용자 유효성 검증 쿼리(prafta-051-11).
 *
 * <p>대상이 세션 사업장(siteCd) 소속 활성 사용자인지(정규직/일용직) 서버에서 재확인한다.
 * 일용직은 C7 만료/탈퇴 필터까지 재검증해 만료자 입실을 차단한다.
 */
public record EntryTargetQuery(
	String gvCmpnyCd
	, String siteCd
	, String userTypeCd
	, String userCd
){

}
