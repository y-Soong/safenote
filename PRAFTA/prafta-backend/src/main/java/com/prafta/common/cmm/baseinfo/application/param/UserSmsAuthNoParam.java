package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.UserSmsAuthNoRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserSmsAuthNoParam (
	String cmpnyCd
    , String mblNo
    , String dupChkYn
    // SMS2-B4: 요청 IP 해시(IP 축 상한 재료). 컨트롤러가 SmsClientIpResolver 로 해석해 넣는다.
    // ★서비스 계층이 HttpServletRequest 에 직접 의존하지 않게 하려는 경계다(AuditContext 선례).
    // ★확정하지 못하면 null 이며 그때는 IP 축을 판정하지 않는다(fail-open).
    , String ipHash
) {
	public static UserSmsAuthNoParam from(UserSmsAuthNoRequest request, String ipHash) {

		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new UserSmsAuthNoParam(
			request.getCmpnyCd()
			, request.getMblNo()
			, request.getDupChkYn()
			, ipHash
		);
	}
}
