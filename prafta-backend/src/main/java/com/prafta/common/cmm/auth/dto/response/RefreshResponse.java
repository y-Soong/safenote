package com.prafta.common.cmm.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshResponse {
	// 신규 발급된 액세스 토큰(JWT)
	String token;
	// 회전(rotation)으로 새로 발급된 refresh token 평문.
	// 프론트 useAuth 는 res.data.refreshToken 을 받아 localStorage 에 저장한다.
	String refreshToken;
}
