package com.prafta.common.cmm.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.auth.mapper.AuthMapper;
import com.prafta.common.cmm.auth.service.AuthService;
import com.prafta.common.cmm.auth.vo.AuthToken;
import com.prafta.common.cmm.auth.vo.UserInfo;
import com.prafta.common.cmm.login.mapper.LoginMapper;
import com.prafta.common.exception.UnauthorizedException;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{
	
	private final AuthMapper authMapper; // TB_AUTH_TOKEN 조회/업데이트
	private final JwtUtil jwtUtil;

	public String refreshAccessToken(String refreshToken) {

	    // 0) refreshToken 값 검증
	    if (refreshToken == null || refreshToken.isBlank()) {
	        throw new UnauthorizedException("NO_REFRESH_TOKEN");
	    }

	    // 1) refreshTokenHash 생성 (로그인과 동일 규칙!)
	    String pepper = "서버만_아는_비밀값"; // TODO: env/yml 로 이동
	    String refreshTokenHash = sha256Hex(refreshToken + ":" + pepper);
	    
	    // 2) DB에서 유효한 refreshTokenHash 조회
	    AuthToken dbToken = authMapper.selectValidByRefreshTokenHash(refreshTokenHash);
	    if (dbToken == null) {
	        throw new UnauthorizedException("INVALID_REFRESH_TOKEN");
	    }

	    UserInfo userInfo = authMapper.selectUserForJwt(dbToken.getUserId());
	    if (userInfo == null) {
	        throw new UnauthorizedException("USER_NOT_FOUND");
	    }

	    return jwtUtil.generateToken(
	        String.valueOf(userInfo.getCmpnyCd()),
	        String.valueOf(userInfo.getUserId()),
	        String.valueOf(userInfo.getUserNm()),
	        String.valueOf(userInfo.getSiteCd()),
	        String.valueOf(userInfo.getSiteNo()),
	        String.valueOf(userInfo.getSiteNm()),
	        String.valueOf(userInfo.getAuthCd()),
	        String.valueOf(userInfo.getMblNo()),
	        String.valueOf(userInfo.getEmail())
	    );
	}
	
	public static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("hash failed", e);
        }
    }
}
