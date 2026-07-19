package com.prafta.web.baim.baim01.result;

import java.math.BigDecimal;

public record SiteInfoResult(
	String chk
	, String siteCd
	, String siteNo
	, String siteNm
	, String cmpnyCd
	, String addr1
	, String addr2
	, String zipCode
	, String strDate
	, String endDate
	, String useYn
	, String siteAdminCd
	, String siteAdminNm
	, String telNo
	, String gpsRange
	, String siteDesc
	, BigDecimal lat
	, BigDecimal lon
	// PRAFTA-SUBCON-T2-04: 연동 원본 회사코드(NULL=일반, NOT NULL=미러 — 배지/편집 비활성 근거).
	// ★record 매핑은 SELECT 컬럼 순서와 일치해야 하므로 반드시 말미 유지.
	, String linkSrcCmpnyCd
){

}
