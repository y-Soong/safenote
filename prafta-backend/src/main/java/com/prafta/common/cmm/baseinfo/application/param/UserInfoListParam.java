package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.UserInfoListRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserInfoListParam(
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
	public static UserInfoListParam from(UserInfoListRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - UserInfoListRequest");
		if (tokenInfo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_003, "\nRequired param missing - TokenInfo");

		return new UserInfoListParam(
			request.getUserId()
			, request.getUserNm()
			, request.getUseYn()
			, request.getSiteCd()
			, request.getNodeCd()
			, request.getSearchMode()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
			, tokenInfo.gv_nodeCd()
		);
	}
}
