package com.prafta.web.baim.baim01.application.model;

public record SiteInfoModel(
		String cmpnyCd
		, String siteCd
		, String siteNo
		, String siteNm
		, String addr1
		, String addr2
		, String zipCode
		, String strDate
		, String endDate
		, String useYn
		, String siteAdminCd
		, String telNo
		, String gpsRange
		, String siteDesc
		
		, String gvCmpnyCd
		, String gvUserCd
) {

}
