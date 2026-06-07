package com.prafta.web.tbm.tbm02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm02.dto.request.ManagerEnterRequest;

/**
 * 관리자 직접 입실 파라미터(prafta-051-11).
 *
 * <p>sessionCd/userTypeCd/userCd 는 요청에서, 회사/사업장/권한/처리자 식별자는 JWT 에서만 도출한다.
 */
public record ManagerEnterParam(
	String sessionCd
	, String userTypeCd
	, String userCd
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
	, String gvUserCd
){
	public static ManagerEnterParam from(ManagerEnterRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new ManagerEnterParam(
			request.getSessionCd()
			, request.getUserTypeCd()
			, request.getUserCd()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
			, tokenInfo.gv_userCd()
		);
	}
}
