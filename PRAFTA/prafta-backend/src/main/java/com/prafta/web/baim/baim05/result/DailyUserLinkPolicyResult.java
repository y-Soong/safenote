package com.prafta.web.baim.baim05.result;

public record DailyUserLinkPolicyResult (
	String chk
	, String cmpnyCd
	, String siteCd
	, String siteNo
	, String siteNm
	, String useYn
	, String oriUseYn
	, String dayLimitCnt
	, String joinCd
	// PRAFTA_COM_001 T1-06: 활성화 계정 수(현재 점유 중 슬롯 SLOT_STATUS='02' COUNT).
	, Integer activeAccountCount
){

}
