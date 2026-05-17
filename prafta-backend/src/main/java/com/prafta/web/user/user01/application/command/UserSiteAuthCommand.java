package com.prafta.web.user.user01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.model.UserInfoModel;

public record UserSiteAuthCommand(
	String cmpnyCd
    , String userCd
    , String siteCd
    , String useYn
    , String gvUserCd
){
	public static UserSiteAuthCommand from(UserInfoModel model) {

    if (model == null) 
    	throw new ApiException(CommonErrorCode.COMMON_400_001);

    return new UserSiteAuthCommand(
		model.cmpnyCd()
		, model.userCd()
		, model.siteCd()
		, model.useYn()
		, model.gvUserCd()
	);
}
}
