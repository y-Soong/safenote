package com.prafta.web.chkLst.chkLst04.result;

public record ChkptTargetResult(
	String chkptCd							// 점검대상코드
	, String chkptNm						// 점검대상명칭
	, String mgmtUserNm						// 관리자명(MGMT_USER_CD -> USER_NM)
	, String useYn							// 사용여부(Y/N) - PRAFTA_COM_001-T5-12.2
	, String chkptDesc						// 비고
) {

}
