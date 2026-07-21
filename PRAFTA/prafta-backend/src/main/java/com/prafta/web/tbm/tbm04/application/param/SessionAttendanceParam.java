package com.prafta.web.tbm.tbm04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm04.dto.request.SessionAttendanceRequest;

public record SessionAttendanceParam(
	String sessionCd
	, String userTypeCd
	, String completionStatusCd
	, boolean includeEventSummary
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
	, String gvUserCd
){
	public static SessionAttendanceParam from(SessionAttendanceRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		// 이상신호 요약 포함 여부 기본값 true(미지정 시)
		boolean includeEventSummary = request.getIncludeEventSummary() == null
				|| request.getIncludeEventSummary();

		return new SessionAttendanceParam(
			request.getSessionCd()
			, request.getUserTypeCd()
			, request.getCompletionStatusCd()
			, includeEventSummary
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
			, tokenInfo.gv_userCd()
		);
	}
}
