package com.prafta.web.user.user01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.param.UserInfoListParam;

public record UserInfoListQuery (
	String userId
	, String userNm
	, String userKeyword
	, String useYn
	, String siteCd
	, String nodeCd
	, String incSubNodeYn
	, String employmentType
	, String gvCmpnyCd
	, String gvUserCd
	, String gvNodeCd
){
public static UserInfoListQuery from(UserInfoListParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new UserInfoListQuery(
			param.userId()
			, param.userNm()
			, param.userKeyword()
			, param.useYn()
			, param.siteCd()
			, param.nodeCd()
			, param.incSubNodeYn()
			, param.employmentType()
			, param.gvCmpnyCd()
			, param.gvUserCd()
			, param.gvNodeCd()
		);
	}
}
