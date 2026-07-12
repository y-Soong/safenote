package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.UserInfoListRequest;

public record UserInfoListParam (
	String userId
	, String userNm
	, String userKeyword
	, String useYn
	, String siteCd
	, String nodeCd
	, String incSubNodeYn
	, String searchMode
	, String employmentType
	, String gvCmpnyCd
	, String gvUserCd
	, String gvNodeCd
){
	public static UserInfoListParam from(UserInfoListRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);
		
		return new UserInfoListParam(
			request.getUserId()
			, request.getUserNm()
			, request.getUserKeyword()
			, request.getUseYn()
			, request.getSiteCd()
			, request.getNodeCd()
			, request.getIncSubNodeYn()
			, request.getSearchMode()
			, request.getEmploymentType()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
			, tokenInfo.gv_nodeCd()
		);
	}
}
