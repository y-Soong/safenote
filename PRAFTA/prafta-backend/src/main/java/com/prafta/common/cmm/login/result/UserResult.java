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
	, String useYn // PRAFTA-app-027-2': 통합형 일용직 로그인 시 TB_USER 비활성('N') 이중 차단용
	, String employmentType // prafta-app-025 J1-4: 고용형태[SYS041] REGULAR/CONTRACT/DAILY/EXECUTIVE (일용직 화면 숨김 신호)
	, String insertNo
	, String insertDate
	, String updateNo
	, String updateDate
) {

}
