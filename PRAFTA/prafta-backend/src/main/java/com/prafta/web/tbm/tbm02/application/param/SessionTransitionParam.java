package com.prafta.web.tbm.tbm02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm02.dto.request.SessionTransitionRequest;

/**
 * 단순 상태 전이 파라미터(prafta-051).
 *
 * <p>교육시작/연장/교육종료/종료비번재발급에 공용. sessionCd + 세션 식별/권한용 gv_* 클레임.
 */
public record SessionTransitionParam(
	String sessionCd
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
	, String gvUserCd
){
	public static SessionTransitionParam from(SessionTransitionRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new SessionTransitionParam(
			request.getSessionCd()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
			, tokenInfo.gv_userCd()
		);
	}
}
