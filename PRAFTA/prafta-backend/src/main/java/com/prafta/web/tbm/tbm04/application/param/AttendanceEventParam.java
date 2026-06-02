package com.prafta.web.tbm.tbm04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm04.dto.request.AttendanceEventRequest;

public record AttendanceEventParam(
	String attendanceCd
	, int page
	, int pageSize
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
){
	public static AttendanceEventParam from(AttendanceEventRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		int page = (request.getPage() == null || request.getPage() < 1) ? 1 : request.getPage();
		// 이벤트 1세션 수백건 가능 → 페이지 크기 기본 100
		int pageSize = (request.getPageSize() == null || request.getPageSize() < 1) ? 100 : request.getPageSize();

		return new AttendanceEventParam(
			request.getAttendanceCd()
			, page
			, pageSize
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
		);
	}
}
