package com.prafta.common.cmm.login.application.param;

import com.prafta.common.cmm.login.dto.request.LoginRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record LoginParam(
	String userId
	, String userPw
	, String clientType
	// ===== prafta-com-003 C3: 디바이스 식별/메타(부정탐지 baseline) — 모두 nullable =====
	, String deviceId
	, String deviceType
	, String deviceModel
	, String osVersion
	, String appVersion
	, String ipAddr
) {
	public static LoginParam from(LoginRequest request, String clientType, String ipAddr) {

		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(clientType == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new LoginParam(
            request.getUserId()
            , request.getUserPw()
            , clientType
            , normalize(request.getDeviceId(), 100)
            , normalize(request.getDeviceType(), 20)
            , normalize(request.getDeviceModel(), 50)
            , normalize(request.getOsVersion(), 20)
            , normalize(request.getAppVersion(), 20)
            , ipAddr
        );
    }

    /**
     * prafta-com-003 C3: 디바이스 메타 정규화 — 트림 후 빈값이면 null, 컬럼 길이 초과분 컷.
     * 클라 제공값(위조 가능)이라 식별/인가에는 쓰지 않고 이력/디바이스 상태 적재용으로만 사용한다.
     */
    private static String normalize(String v, int maxLen) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > maxLen ? t.substring(0, maxLen) : t;
    }
}
