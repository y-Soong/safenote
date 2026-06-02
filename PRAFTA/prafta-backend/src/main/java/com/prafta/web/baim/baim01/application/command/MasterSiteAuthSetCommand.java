package com.prafta.web.baim.baim01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim01.application.model.SiteInfoModel;

public record MasterSiteAuthSetCommand(
	String cmpnyCd
	, String siteCd
	, String gvUserCd
) {
	public static MasterSiteAuthSetCommand from(SiteInfoModel model, String siteCd) {

		if(model == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(siteCd == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		// PRAFTA-042 (H-2): 테넌트 격리. cmpnyCd 는 저장 컨텍스트의 gvCmpnyCd(JWT) 사용(body 아님).
		return new MasterSiteAuthSetCommand(
			model.gvCmpnyCd()
			, siteCd
			, model.gvUserCd()
		);
	}
}
