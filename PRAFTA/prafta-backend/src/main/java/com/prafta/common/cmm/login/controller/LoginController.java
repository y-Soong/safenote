package com.prafta.common.cmm.login.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.cmm.login.application.param.AuthMenuInfoParam;
import com.prafta.common.cmm.login.application.param.LoginParam;
import com.prafta.common.cmm.login.application.param.LogoutParam;
import com.prafta.common.cmm.login.application.param.UserJoinParam;
import com.prafta.common.cmm.login.application.param.UserTermsAgreementCheckParam;
import com.prafta.common.cmm.login.application.param.VerifyPhoneAuthParam;
import com.prafta.common.cmm.login.dto.request.AuthMenuInfoRequest;
import com.prafta.common.cmm.login.dto.request.LoginRequest;
import com.prafta.common.cmm.login.dto.request.UserJoinRequest;
import com.prafta.common.cmm.login.dto.request.VerifyPhoneAuthRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.login.LoginErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.cmm.login.dto.response.AuthLogoutResponse;
import com.prafta.common.cmm.login.dto.response.LoginResponse;
import com.prafta.common.cmm.login.dto.response.UserTermsAgreementCheckResponse;
import com.prafta.common.cmm.login.service.LoginService;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.ClientIpExtractor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@Validated
@RestController
@RequestMapping("/login")
@RequiredArgsConstructor // 롬복이 final 필드로 생성자 자동 생성
public class LoginController { 	
	
	private final JwtUtil jwtUtil;
	private final LoginService loginService;
	
    // 보안 수정(PRAFTA-006-001): 자격증명이 URL 쿼리스트링/서버 로그/Referer에 노출되지 않도록 POST + JSON 본문으로 전환
    @PostMapping("/login")
    public ResponseEntity<?> Login(@Valid @RequestBody LoginRequest loginRequest, @RequestHeader(value = "X-Client-Type", required = false, defaultValue = "WEB") String clientType, HttpServletRequest httpRequest) {
    	// prafta-com-003 C3: 로그인 IP 는 서버가 추출(디바이스 로그인 이력 LOGIN_IP). 클라 신뢰 안 함.
    	String ipAddr = ClientIpExtractor.extract(httpRequest);
    	LoginResponse loginResponse = loginService.Login(LoginParam.from(loginRequest, clientType, ipAddr));

    	return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }

    /**
     * 로그아웃: 활성 세션 revoke (B안: 일반 API는 JWT만 검증, refresh에서만 DB검증)
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthLogoutResponse> logout(
		@RequestHeader(value = "X-Client-Type", required = false, defaultValue = "WEB") String clientType
		, @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
    	loginService.logout(LogoutParam.from(clientType, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
    
    @PostMapping("/insert-user-info")
    public ResponseEntity<?> insertUserInfo(@Valid @RequestBody UserJoinRequest request) {
    	
    	loginService.insertUserInfo(UserJoinParam.from(request));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
    
    @GetMapping("/user-terms-agreement-check")
    public ResponseEntity<?> userTermsAgrementCheck(@RequestHeader("Authorization") String authorization) {
    	UserTermsAgreementCheckResponse response = loginService.userTermsAgrementCheck(UserTermsAgreementCheckParam.from(jwtUtil.getAllClaimsAsMap(authorization)));
   	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @PostMapping("/update-auth-menu-info")
    public ResponseEntity<?> updateAuthMenuInfo(@RequestBody List<AuthMenuInfoRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	loginService.updateAuthMenuInfo(AuthMenuInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * PRAFTA-036 — 인증대기('04') 계정의 휴대폰 본인인증 후 활성화.
     *
     * <p>{@code Authorization: Bearer <임시 scope=PHONE_AUTH 토큰>} 필수.
     * 인증 성공 시 정식 토큰/리프레시 + LoginResponse 응답(일반 로그인과 동일 형식).
     */
    @PostMapping("/verify-phone-auth")
    public ResponseEntity<?> verifyPhoneAuth(
            @Valid @RequestBody VerifyPhoneAuthRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization,
            @RequestHeader(value = "X-Client-Type", required = false, defaultValue = "WEB") String clientType) {

        // 임시 토큰 검증 (만료/서명) — 일반 토큰 검증 흐름 재사용.
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiException(LoginErrorCode.LOGIN_400_012);
        }
        String pureToken = authorization.substring(7);
        if (!jwtUtil.validateToken(pureToken)) {
            throw new ApiException(LoginErrorCode.LOGIN_400_012);
        }

        // 임시 토큰의 scope claim 추출.
        String scope = jwtUtil.parseToken(pureToken).get("gv_scope", String.class);
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        LoginResponse response = loginService.verifyPhoneAuth(
                VerifyPhoneAuthParam.from(request, tokenInfo, scope, clientType));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
