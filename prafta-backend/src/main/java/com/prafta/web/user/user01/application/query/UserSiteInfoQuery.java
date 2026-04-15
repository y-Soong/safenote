package com.prafta.web.user.user01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.model.UserInfoModel;


public record UserSiteInfoQuery(
	String cmpnyCd
	, String userCd
){
	public static UserSiteInfoQuery from(UserInfoModel model) {

	        if (model == null) {
	        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserInfoModel");
        }

        return new UserSiteInfoQuery(
    		model.cmpnyCd()
    		, model.userCd()
		);
    }
}