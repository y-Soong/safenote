package com.prafta.web.baim.baim01.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SiteInfo{
	String chk;
	String siteCd;
	String siteNo;
	String siteNm;
	String cmpnyCd;
	String addr1;
	String addr2;
	String zipCode;
	String strDate;
	String endDate;
	String useYn;
	String siteAdminId;
	String siteAdminNm;
	String telNo;
	String gpsRange;
	String siteDesc;
}
