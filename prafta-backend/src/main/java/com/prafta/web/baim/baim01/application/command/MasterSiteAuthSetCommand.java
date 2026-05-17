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
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(siteCd == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new MasterSiteAuthSetCommand(
			siteCd
			, model.gvUserCd()
		); 
	}
}
