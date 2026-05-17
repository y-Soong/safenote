package com.prafta.web.baim.baim01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim01.application.model.SiteInfoModel;

public record SiteNodeInfoCommand(
	String siteCd
	, String nodeCd
	, String nodeNm
	, String nodeType
	, String gvCmpnyCd
	, String gvUserCd
) {
	public static SiteNodeInfoCommand from(SiteInfoModel model, String siteCd) {
		
		if(model == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(siteCd == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new SiteNodeInfoCommand(
			siteCd
			, "n1"
			, model.siteNm()
			, "00001"
			, model.gvCmpnyCd()
			, model.gvUserCd()
		); 
	}
}

