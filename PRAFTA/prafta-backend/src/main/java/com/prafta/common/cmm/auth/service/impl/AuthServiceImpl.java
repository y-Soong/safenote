package com.prafta.common.cmm.auth.service.impl;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.auth.application.param.RefreshParam;
import com.prafta.common.cmm.auth.dto.response.RefreshResponse;
import com.prafta.common.cmm.auth.mapper.AuthMapper;
import com.prafta.common.cmm.auth.result.AuthTokenResult;
import com.prafta.common.cmm.auth.service.AuthService;
import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.error.auth.AuthErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.security.crypto.HmacSigner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

	private final AuthMapper authMapper; // TB_AUTH_TOKEN 조회/업데이트
	private final JwtUtil jwtUtil;
	private final HmacSigner hmacSigner;

	// grace window 길이(초). 탭A 회전 직후 탭B 가 옛 RT 로 refresh 시도하는 경우를 완충한다.
	// TB_AUTH_TOKEN.REVOKED_DTIME 기준 최근 GRACE_SECONDS 초 이내 폐기 건을 1회 허용한다.
	private static final int GRACE_SECONDS = 60;

	@Override
	@Transactional
	public RefreshResponse refreshAccessToken(RefreshParam param) {

	    // 0) refreshToken 값 검증
	    if (param == null || param.refreshToken() == null || param.refreshToken().isBlank()) {
	        throw new ApiException(AuthErrorCode.AUTH_500_001);
	    }

	    // 1) refreshTokenHash 생성 (로그인과 동일 규칙)
	    String refreshTokenHash = hmacSigner.hmacSha256Base64Url(param.refreshToken());

	    // 2) 유효한 refresh token 조회 → 없으면 grace window(최근 폐기 건) 재시도
	    AuthTokenResult authTokenResult = authMapper.selectValidByRefreshTokenHash(refreshTokenHash);
	    if (authTokenResult == null) {
	        authTokenResult = authMapper.selectRecentlyRevokedByRefreshTokenHash(refreshTokenHash, GRACE_SECONDS);
	        if (authTokenResult != null) {
	            // 멀티 탭 완충: 직전 회전으로 이미 폐기된 RT 이지만 grace 시간 내라 1회 허용한다.
	            log.info("refresh - grace window 적용, tokenId={}", authTokenResult.tokenId());
	        }
	    }
	    if (authTokenResult == null) {
	        throw new ApiException(AuthErrorCode.AUTH_500_002);
	    }

	    UserResult userResult = authMapper.selectUserForJwt(authTokenResult.userCd());
	    if (userResult == null) {
	        throw new ApiException(AuthErrorCode.AUTH_500_003);
	    }

	    // 3) Refresh Token 회전(rotation)
	    //    - 기존 RT row 를 폐기(REVOKED_YN='Y')하고 신규 RT row 를 발급한다.
	    //    - 신규 RT 의 EXPIRE_DTIME 은 기존 토큰의 만료시각을 그대로 승계한다.
	    //      (회전이 RT 절대 수명을 연장하지 못하도록 - 정책 §3.4 48시간 상한 유지)
	    String newRefreshToken = generateRefreshToken();
	    String newRefreshTokenHash = hmacSigner.hmacSha256Base64Url(newRefreshToken);
	    String newTokenId = UUID.randomUUID().toString().replace("-", "");

	    // 3-1) 기존 RT 폐기 (유효 RT 경로일 때만 실제 갱신, grace 경로는 이미 폐기 상태)
	    authMapper.revokeTokenById(
	            authTokenResult.cmpnyCd()
	            , authTokenResult.userCd()
	            , authTokenResult.tokenId());

	    // 3-2) 신규 RT insert (UX_AUTH_TOKEN_RTH UNIQUE 충돌 방어)
	    try {
	        authMapper.insertRotatedToken(
	                authTokenResult.cmpnyCd()
	                , authTokenResult.userCd()
	                , newTokenId
	                , authTokenResult.clientType()
	                , authTokenResult.deviceId()
	                , newRefreshTokenHash
	                , authTokenResult.expireDtime());
	    } catch (DuplicateKeyException e) {
	        // 64바이트 SecureRandom 이라 현실적으로 발생 불가하나 방어적으로 처리한다.
	        log.error("refresh - refresh token 해시 충돌", e);
	        throw new ApiException(AuthErrorCode.AUTH_500_002);
	    }

	    // 4) 신규 액세스 토큰(JWT) 발급
	    //    정책 §11.1 에 따라 휴대폰/이메일은 JWT 에 포함하지 않는다.
	    String token = jwtUtil.generateToken(userResult);

	    log.info("refresh - 토큰 회전 완료, userCd={}, newTokenId={}", authTokenResult.userCd(), newTokenId);

	    return RefreshResponse.builder()
	                          .token(token)
	                          .refreshToken(newRefreshToken)
	                          .build();
	}

	// refresh token 평문 생성 (로그인 LoginServiceImpl 과 동일 규칙: SecureRandom 64바이트 → Base64 URL, no padding)
	private String generateRefreshToken() {
	    SecureRandom random = new SecureRandom();
	    byte[] bytes = new byte[64];
	    random.nextBytes(bytes);
	    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
