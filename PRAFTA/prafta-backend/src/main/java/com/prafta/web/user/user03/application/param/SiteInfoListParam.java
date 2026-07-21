package com.prafta.web.user.user03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.user.UserErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.user.user03.dto.request.SiteInfoListRequest;

public record SiteInfoListParam(
	String userCd
	, String gvCmpnyCd
) {
	public static SiteInfoListParam from(SiteInfoListRequest request, TokenInfo tokenInfo) {

		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);
		// [보안 H-1] 사업장 권한 관리 화면(User_03) 조회 — 관리자 역할(master/hr/safe/system)만 허용.
		if (!AuthRoleUtils.canManageSite(tokenInfo.gv_authCd()))
			throw new ApiException(UserErrorCode.USER_403_002);

		return new SiteInfoListParam(
			request.getUserCd()
			, tokenInfo.gv_cmpnyCd()
		); 
	}
}
