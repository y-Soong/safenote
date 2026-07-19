package com.prafta.web.tbm.tbm02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm02.dto.request.SharedSessionListRequest;

/**
 * 연동받은 교육 목록 파라미터(PRAFTA-SUBCON-T5 D2).
 *
 * <p>필터/페이징만 요청에서 받고, 회사/권한 식별자는 JWT 에서만 도출한다.
 */
public record SharedSessionListParam(
	String statusCd
	, String searchKeyword
	, int page
	, int pageSize
	, String gvCmpnyCd
	, String gvAuthCd
){
	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = 100;

	public static SharedSessionListParam from(SharedSessionListRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		int page = (request.getPage() == null || request.getPage() < 1) ? 1 : request.getPage();
		int pageSize = (request.getPageSize() == null || request.getPageSize() < 1)
				? DEFAULT_PAGE_SIZE : request.getPageSize();
		if (pageSize > MAX_PAGE_SIZE) {
			pageSize = MAX_PAGE_SIZE;
		}

		return new SharedSessionListParam(
			request.getStatusCd()
			, request.getSearchKeyword()
			, page
			, pageSize
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_authCd()
		);
	}
}
