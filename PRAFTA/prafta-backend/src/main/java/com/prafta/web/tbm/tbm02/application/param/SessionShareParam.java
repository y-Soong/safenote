package com.prafta.web.tbm.tbm02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm02.dto.request.SessionShareRequest;

/**
 * 연동 회사 지정/해제/조회 파라미터(PRAFTA-SUBCON-T5).
 *
 * <p>sessionCd/shareCmpnyCd 는 요청에서, 회사/사업장/권한/행위자 식별자는 JWT 에서만 도출한다.
 * 개설사(HOST_CMPNY_CD)는 서버가 세션에서 읽으며 요청으로 받지 않는다.
 */
public record SessionShareParam(
	String sessionCd
	, String shareCmpnyCd
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
	, String gvUserCd
){
	public static SessionShareParam from(SessionShareRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new SessionShareParam(
			request.getSessionCd()
			, request.getShareCmpnyCd()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
			, tokenInfo.gv_userCd()
		);
	}
}
