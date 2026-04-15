package com.prafta.common.cmm.auth.service.impl;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.auth.application.param.RefreshParam;
import com.prafta.common.cmm.auth.dto.response.RefreshResponse;
import com.prafta.common.cmm.auth.mapper.AuthMapper;
import com.prafta.common.cmm.auth.result.AuthTokenResult;
import com.prafta.common.cmm.auth.service.AuthService;
import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.error.auth.AuthErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{
	
	private final AuthMapper authMapper; // TB_AUTH_TOKEN 조회/업데이트
	private final JwtUtil jwtUtil;
	private final AesGcmCrypto aesGcmCrypto;
	private final HmacSigner hmacSigner;

	public RefreshResponse refreshAccessToken(RefreshParam param) {
		
		RefreshResponse response = null;

	    // 0) refreshToken 값 검증
	    if (param == null || param.refreshToken().isBlank()) {
	        throw new ApiException(AuthErrorCode.AUTH_500_001);
	    }

	    // 1) refreshTokenHash 생성 (로그인과 동일 규칙!)
	    String refreshTokenHash = hmacSigner.hmacSha256Base64Url(param.refreshToken());
	    
	    // 2) DB에서 유효한 refreshTokenHash 조회
	    AuthTokenResult authTokenResult = authMapper.selectValidByRefreshTokenHash(refreshTokenHash);
	    if (authTokenResult == null) {
	        throw new ApiException(AuthErrorCode.AUTH_500_002);
	    }

	    UserResult userResult = authMapper.selectUserForJwt(authTokenResult.userCd());
	    if (userResult == null) {
	        throw new ApiException(AuthErrorCode.AUTH_500_003);
	    }
	    
	    String mblNo = aesGcmCrypto.decrypt(userResult.mblNoEnc());
		String email = aesGcmCrypto.decrypt(userResult.emailEnc());
		String token = jwtUtil.generateToken(userResult, mblNo, email); 

		response = RefreshResponse.builder()
									.token(token)
									.build();
		
	    return response;
	}
}
