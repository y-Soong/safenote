package com.prafta.common.cmm.login.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
import com.prafta.common.cmm.login.dto.request.AuthMenuInfoRequest;
import com.prafta.common.cmm.login.dto.request.LoginRequest;
import com.prafta.common.cmm.login.dto.request.UserJoinRequest;
import com.prafta.common.cmm.login.dto.response.AuthLogoutResponse;
import com.prafta.common.cmm.login.dto.response.LoginResponse;
import com.prafta.common.cmm.login.dto.response.UserTermsAgreementCheckResponse;
import com.prafta.common.cmm.login.service.LoginService;
import com.prafta.common.security.JwtUtil;

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
	
    @GetMapping("/login")
    public ResponseEntity<?> Login(@Valid @ModelAttribute LoginRequest loginRequest, @RequestHeader(value = "X-Client-Type", required = false, defaultValue = "WEB") String clientType) {
    	LoginResponse loginResponse = loginService.Login(LoginParam.from(loginRequest, clientType));
    	
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
}
