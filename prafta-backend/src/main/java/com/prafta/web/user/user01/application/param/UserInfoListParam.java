package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.UserInfoListRequest;

public record UserInfoListParam (
	String userId
	, String userNm
	, String useYn
	, String siteCd
	, String nodeCd
	, String searchMode
	, String gvCmpnyCd
	, String gvUserCd
	, String gvNodeCd
){
	public static UserInfoListParam from(UserInfoListRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserInfoListRequest");
		if(tokenInfo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_003,"\n필수값 누락 - TokenInfo");
		
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
