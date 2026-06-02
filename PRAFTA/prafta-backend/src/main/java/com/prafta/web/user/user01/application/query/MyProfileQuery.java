package com.prafta.web.user.user01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.param.MyProfileParam;

public record MyProfileQuery(
	String gvCmpnyCd
	, String gvUserCd
){
	public static MyProfileQuery from(MyProfileParam param) {

		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new MyProfileQuery(
			param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
