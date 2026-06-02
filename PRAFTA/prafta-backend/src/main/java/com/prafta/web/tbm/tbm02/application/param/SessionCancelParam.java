package com.prafta.web.tbm.tbm02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm02.dto.request.SessionCancelRequest;

public record SessionCancelParam(
	String sessionCd
	, String cancelReason
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
	, String gvUserCd
){
	public static SessionCancelParam from(SessionCancelRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new SessionCancelParam(
			request.getSessionCd()
			, request.getCancelReason()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
			, tokenInfo.gv_userCd()
		);
	}
}
