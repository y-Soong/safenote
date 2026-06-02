package com.prafta.common.cmm.login.result;

public record UserResult(
	String cmpnyCd
	, String userCd
	, String userId
	, String userNm
	, String userPw
	, String authCd
	, String authLevel
	, String siteCd
	, String siteNo
	, String siteNm
	, String nodeCd
	, String nodeNm
	, String mblNoEnc
	, String emailEnc
	, String pwdLockYn
	, String pwdFailCnt
	, String pwdLockExpireDtime
	, String pwdChgDtime
	, String withdrawalDate
	, String lastLoginDtime
	, String accountStatus // PRAFTA-036: '04'=인증대기 분기에 사용
	, String insertNo
	, String insertDate
	, String updateNo
	, String updateDate
) {

}
