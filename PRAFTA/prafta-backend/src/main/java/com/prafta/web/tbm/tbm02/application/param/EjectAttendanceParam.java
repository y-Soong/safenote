package com.prafta.web.tbm.tbm02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm02.dto.request.EjectAttendanceRequest;

/**
 * 입실자 내보내기 파라미터(prafta-051-12).
 *
 * <p>sessionCd/attendanceCd/reason 은 요청에서, 회사/사업장/권한/처리자 식별자는 JWT 에서만 도출한다.
 */
public record EjectAttendanceParam(
	String sessionCd
	, String attendanceCd
	, String reason
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
	, String gvUserCd
){
	public static EjectAttendanceParam from(EjectAttendanceRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new EjectAttendanceParam(
			request.getSessionCd()
			, request.getAttendanceCd()
			, request.getReason()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
			, tokenInfo.gv_userCd()
		);
	}
}
