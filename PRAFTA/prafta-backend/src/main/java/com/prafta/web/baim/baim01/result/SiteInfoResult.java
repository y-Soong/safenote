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
){
	
}
