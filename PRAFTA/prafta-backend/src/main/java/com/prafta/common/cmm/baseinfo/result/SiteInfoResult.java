package com.prafta.common.cmm.baseinfo.result;

public record SiteInfoResult(
	String siteCd
	, String siteNo
	, String siteNm
	, String siteAdminCd
	, String siteAdminNm
	, String addr1
	, String addr2
	, String telNo
	// PRAFTA-SUBCON-T2-09: 연동 원본 회사코드(NULL=일반, NOT NULL=미러 — 화면 편집 비활성 판정용).
	// ★record 매핑은 SELECT 컬럼 순서와 일치해야 하므로 반드시 말미 유지.
	, String linkSrcCmpnyCd
) {

}
