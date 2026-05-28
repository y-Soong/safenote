package com.prafta.web.tbm.tbm04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm04.dto.request.CompletionUpdateRequest;

public record CompletionUpdateParam(
	String attendanceCd
	, String completionStatusCd
	, String reason
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvUserCd
	, String gvAuthCd
){
	public static CompletionUpdateParam from(CompletionUpdateRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new CompletionUpdateParam(
			request.getAttendanceCd()
			, request.getCompletionStatusCd()
			, request.getReason()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_userCd()
			, tokenInfo.gv_authCd()
		);
	}
}
