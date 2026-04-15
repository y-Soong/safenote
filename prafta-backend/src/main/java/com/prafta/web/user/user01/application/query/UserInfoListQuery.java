package com.prafta.web.user.user01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.param.UserInfoListParam;

public record UserInfoListQuery (
	String userId
	, String userNm
	, String useYn
	, String siteCd
	, String nodeCd
	, String gvCmpnyCd
	, String gvUserCd
	, String gvNodeCd
){
public static UserInfoListQuery from(UserInfoListParam param) {
		
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserInfoListQuery");
		
		return new UserInfoListQuery(
			param.userId()
			, param.userNm()
			, param.useYn()
			, param.siteCd()
			, param.nodeCd()
			, param.gvCmpnyCd()
			, param.gvUserCd()
			, param.gvNodeCd()
		);
	}
}
