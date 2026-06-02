package com.prafta.web.baim.baim01.application.command;

import java.math.BigDecimal;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim01.application.model.SiteInfoModel;

public record SiteInfoCommand(
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
	, BigDecimal lat
	, BigDecimal lon
	, String gvCmpnyCd
	, String gvUserCd
) {
	public static SiteInfoCommand from(SiteInfoModel model, String siteCd) {
		
		if(model == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(siteCd == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new SiteInfoCommand(
			model.cmpnyCd()
			, siteCd
			, model.siteNo()
			, model.siteNm()
			, model.addr1()
			, model.addr2()
			, model.zipCode()
			, model.strDate()
			, model.endDate()
			, model.useYn()
			, model.siteAdminCd()
			, model.telNo()
			, model.gpsRange()
			, model.siteDesc()
			, model.lat()
			, model.lon()
			, model.gvCmpnyCd()
			, model.gvUserCd()
		); 
	}
}

