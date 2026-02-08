package com.prafta.common.cmm.login.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthLogoutReq {
	/**
     * WEB / APP
     */
    private String clientType;

    /**
     * 앱에서만 의미있음(선택)
     */
    private String deviceId;

    /**
     * Y면 clientType 상관없이 해당 유저의 활성 세션 전체 revoke
     * 기본은 N
     */
    @Builder.Default
    private String logoutAllYn = "N";
}
