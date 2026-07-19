package com.prafta.web.chkLst.chkLst01.result;

public record ChkptResult(
	String chk
	, String cmpnyCd
	, String siteCd
	, String siteNm
	, String chkLstType
	, String chkptCd
	, String chkptNm
	, String chkptDesc
	, String useYn
	, String mgmtUserCd
	, String mgmtUserNm
	// PRAFTA-SUBCON-T6-03: 연동 원본 회사코드(NULL=자체, NOT NULL=미러 → 화면 '연동' 배지 + 읽기전용).
	//   record 위치 매핑이라 신규 필드는 SELECT 말미와 같은 순서로 말미에 추가한다.
	, String linkSrcCmpnyCd
){

}
