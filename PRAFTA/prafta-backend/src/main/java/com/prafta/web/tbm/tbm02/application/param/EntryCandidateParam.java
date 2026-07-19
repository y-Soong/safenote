package com.prafta.web.tbm.tbm02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm02.dto.request.EntryCandidateRequest;

/**
 * 입실 후보 검색 파라미터(prafta-051-11).
 *
 * <p>sessionCd/userTypeCd/keyword 는 요청에서, 회사/사업장/권한 식별자는 JWT 에서만 도출한다.
 */
public record EntryCandidateParam(
	String sessionCd
	, String userTypeCd
	, String keyword
	// PRAFTA-SUBCON-T5: 후보 검색 대상 회사(미지정이면 자사). 서버 게이트가 체인 소속을 재검증한다.
	, String targetCmpnyCd
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
){
	public static EntryCandidateParam from(EntryCandidateRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new EntryCandidateParam(
			request.getSessionCd()
			, request.getUserTypeCd()
			, request.getKeyword()
			, request.getTargetCmpnyCd()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
		);
	}
}
