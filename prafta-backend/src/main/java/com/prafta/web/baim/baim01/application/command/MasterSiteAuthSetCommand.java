package com.prafta.web.baim.baim01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim01.application.model.SiteInfoModel;

public record MasterSiteAuthSetCommand(
	String siteCd
	, String gvUserCd
) {
	public static MasterSiteAuthSetCommand from(SiteInfoModel model, String siteCd) {
		
		if(model == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteInfoModel");
		if(siteCd == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - siteCd");
		
		return new MasterSiteAuthSetCommand(
			siteCd
			, model.gvUserCd()
		); 
	}
}
