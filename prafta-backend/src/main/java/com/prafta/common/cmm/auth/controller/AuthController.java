package com.prafta.common.cmm.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.cmm.auth.dto.RefreshReq;
import com.prafta.common.cmm.auth.dto.RefreshRes;
import com.prafta.common.cmm.auth.service.AuthService;

import lombok.RequiredArgsConstructor;

@NoAuth
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;
	
	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(@RequestBody RefreshReq dto) {		  

	    String newAccessToken = authService.refreshAccessToken(dto.getRefreshToken());
	    
	    //return ResponseEntity.ok(new RefreshRes(newAccessToken));
	    return ResponseEntity.ok(RefreshRes.builder().token(newAccessToken).build());
	}
}