package com.prafta.web.user.user01.result;

public record UserInfoResult(
	String chk
	, String cmpnyCd
	, String userCd
	, String userId
	, String userNm
	, String authCd
	, String authLevel
	, String siteCd
	, String nodeCd
	, String oriNodeCd
	, String nodeNm
	, String siteNm
	, String mblNo
	, String email
	, String birthDt
	, String gender
	, String useYn
	, String siteNmList
	, String accountStatus
	, String withdrawalDate
	, String rankCd
	, String rankNm
	// PRAFTA-COM-008-E-5 — 기본 근무타입(SCH_CD). UserInfoPop 수정모드 prefill 용. 미설정이면 null.
	, String defaultSchCd
){

}
