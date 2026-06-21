package com.prafta.common.cmm.login.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record LogoutParam(
	String cmpnyCd
	, String userCd
	, String clientType
	, String deviceId
) {
	public static LogoutParam from(String clientType, TokenInfo tokenInfo) {
		return from(clientType, tokenInfo, null);
	}

	/**
	 * prafta-com-015 015-3 — 본문 제공 gv_deviceId 를 우선 사용하는 오버로드.
	 *
	 * <p>앱 로그아웃은 Authorization 헤더 없이 호출될 수 있어(토큰 claim 부재) 본문 gv_deviceId 로
	 * 현재 기기를 식별한다. 본문 값이 비어 있으면 토큰 claim 의 gv_deviceId 로 폴백한다(웹은 둘 다 없음→null).
	 */
	public static LogoutParam from(String clientType, TokenInfo tokenInfo, String bodyDeviceId) {

		if(clientType == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		String deviceId = (bodyDeviceId != null && !bodyDeviceId.isBlank())
				? bodyDeviceId.trim()
				: (tokenInfo != null ? tokenInfo.gv_deviceId() : "");

        return new LogoutParam(
    		tokenInfo != null ? tokenInfo.gv_cmpnyCd() : ""
            , tokenInfo != null ? tokenInfo.gv_userCd() : "SYSTEM"
            , clientType
            , deviceId
        );
    }
}
