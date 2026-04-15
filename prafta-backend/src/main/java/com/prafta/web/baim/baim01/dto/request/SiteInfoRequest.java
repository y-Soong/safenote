package com.prafta.web.baim.baim01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SiteInfoRequest {
	private String cmpnyCd;
	private String siteCd;
	private String siteNo;
	private String siteNm;
	private String addr1;
	private String addr2;
	private String zipCode;
	private String strDate;
	private String endDate;
	private String useYn;
	private String siteAdminCd;
	private String telNo;
	private String gpsRange;
	private String siteDesc;
}
