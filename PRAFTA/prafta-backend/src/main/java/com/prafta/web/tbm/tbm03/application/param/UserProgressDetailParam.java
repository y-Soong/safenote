package com.prafta.web.tbm.tbm03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm03.dto.request.UserProgressDetailRequest;

public record UserProgressDetailParam(
	String userCd
	, String userTypeCd		// REGULAR / DAILY (미지정 시 REGULAR 고정)
	, String startDate
	, String endDate
	, String completionStatusCd
	, int page
	, int pageSize
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
	, String gvUserCd
){
	public static UserProgressDetailParam from(UserProgressDetailRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		int page = (request.getPage() == null || request.getPage() < 1) ? 1 : request.getPage();
		int pageSize = (request.getPageSize() == null || request.getPageSize() < 1) ? 20 : request.getPageSize();

		// 유형 미지정/비정상 값은 정규직으로 고정(일용직은 명시 'DAILY' 만 허용)
		String userTypeCd = "DAILY".equals(request.getUserTypeCd()) ? "DAILY" : "REGULAR";

		return new UserProgressDetailParam(
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
			, tokenInfo.gv_userCd()
		);
	}
}
