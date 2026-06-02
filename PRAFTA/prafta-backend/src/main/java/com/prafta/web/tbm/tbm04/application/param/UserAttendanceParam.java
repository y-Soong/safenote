package com.prafta.web.tbm.tbm04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm04.dto.request.UserAttendanceRequest;

public record UserAttendanceParam(
	String userCd
	, String userTypeCd		// REGULAR / DAILY (엔드포인트에서 고정 주입)
	, String startDate
	, String endDate
	, String completionStatusCd
	, int page
	, int pageSize
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
){
	public static UserAttendanceParam from(UserAttendanceRequest request, TokenInfo tokenInfo, String userTypeCd) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		int page = (request.getPage() == null || request.getPage() < 1) ? 1 : request.getPage();
		int pageSize = (request.getPageSize() == null || request.getPageSize() < 1) ? 20 : request.getPageSize();

		return new UserAttendanceParam(
			request.getUserCd()
			, userTypeCd
			, request.getStartDate()
			, request.getEndDate()
			, request.getCompletionStatusCd()
			, page
			, pageSize
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
		);
	}
}
