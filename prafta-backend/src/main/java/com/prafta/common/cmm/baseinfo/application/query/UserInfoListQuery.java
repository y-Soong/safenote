package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.UserInfoListParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserInfoListQuery(
	String userId
	, String userNm
	, String useYn
	, String siteCd
	, String nodeCd
	, String searchMode
	, String gvCmpnyCd
	, String gvUserCd
	, String gvNodeCd
) {
	public static UserInfoListQuery from(UserInfoListParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new UserInfoListQuery(
			param.userId()
			, param.userNm()
			, param.useYn()
			, param.siteCd()
			, param.nodeCd()
			, param.searchMode()
			, param.gvCmpnyCd()
			, param.gvUserCd()
			, param.gvNodeCd()
		);
	}
}
