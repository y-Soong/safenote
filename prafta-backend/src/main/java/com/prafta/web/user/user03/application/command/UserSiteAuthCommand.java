package com.prafta.web.user.user03.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user03.application.model.UserSiteAuthModel;

public record UserSiteAuthCommand(
	String chk
	, String cmpnyCd
	, String userCd
	, String siteCd
	, String allocYn
	, String useYn
	, String gvCmpnyCd
	, String gvUserCd
) {
	public static UserSiteAuthCommand from(UserSiteAuthModel model) {
		
		if(model == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserSiteAuthModel");
		
		return new UserSiteAuthCommand(
			model.chk()
			, model.cmpnyCd()
			, model.userCd()
			, model.siteCd()
			, model.allocYn()
			, model.useYn()
			, model.gvCmpnyCd()
			, model.gvUserCd()
		); 
	}
}
