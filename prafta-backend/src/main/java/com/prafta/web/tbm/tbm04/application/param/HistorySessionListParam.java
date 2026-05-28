package com.prafta.web.tbm.tbm04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm04.dto.request.HistorySessionListRequest;

public record HistorySessionListParam(
	String siteCd
	, String startDate
	, String endDate
	, String managerUserCd
	, String searchKeyword
	, String statusCd
	, int page
	, int pageSize
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
){
	public static HistorySessionListParam from(HistorySessionListRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		int page = (request.getPage() == null || request.getPage() < 1) ? 1 : request.getPage();
		int pageSize = (request.getPageSize() == null || request.getPageSize() < 1) ? 20 : request.getPageSize();

		return new HistorySessionListParam(
			request.getSiteCd()
			, request.getStartDate()
			, request.getEndDate()
			, request.getManagerUserCd()
			, request.getSearchKeyword()
			, request.getStatusCd()
			, page
			, pageSize
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
		);
	}
}
