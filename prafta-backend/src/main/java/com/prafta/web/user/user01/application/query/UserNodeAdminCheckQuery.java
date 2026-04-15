package com.prafta.web.user.user01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.model.UserInfoModel;

public record UserNodeAdminCheckQuery(
	String cmpnyCd
	, String userCd
){
	public static UserNodeAdminCheckQuery from(UserInfoModel model) {

        if (model == null) {
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserInfoModel");
        }

        return new UserNodeAdminCheckQuery(
    		model.cmpnyCd()
    		, model.userCd()
		);
    }
}
