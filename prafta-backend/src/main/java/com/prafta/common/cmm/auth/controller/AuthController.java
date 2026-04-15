package com.prafta.common.cmm.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.cmm.auth.application.param.RefreshParam;
import com.prafta.common.cmm.auth.dto.request.RefreshRequest;
import com.prafta.common.cmm.auth.dto.response.RefreshResponse;
import com.prafta.common.cmm.auth.service.AuthService;

import lombok.RequiredArgsConstructor;

@NoAuth
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;
	
	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {		  

		RefreshResponse response = authService.refreshAccessToken(RefreshParam.from(request));
	    
	    //return ResponseEntity.ok(new RefreshRes(newAccessToken));
	    return ResponseEntity.ok().body(response);
	}
}