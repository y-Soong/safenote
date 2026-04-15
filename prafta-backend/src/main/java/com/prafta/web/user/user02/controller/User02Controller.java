package com.prafta.web.user.user02.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.user.user02.application.param.AuthMenuInfoParam;
import com.prafta.web.user.user02.application.param.AuthMenuListParam;
import com.prafta.web.user.user02.dto.request.AuthMenuInfoRequest;
import com.prafta.web.user.user02.dto.request.AuthMenuListRequest;
import com.prafta.web.user.user02.dto.response.AuthMenuListResponse;
import com.prafta.web.user.user02.service.User02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@RestController
@RequestMapping("/user02")
@RequiredArgsConstructor
public class User02Controller { 	
	
	private final User02Service user02Service;
	private final JwtUtil jwtUtil;

    @GetMapping("/auth-menu-lists")
    public ResponseEntity<?> getAuthMenuList(@ModelAttribute AuthMenuListRequest reuqest, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	AuthMenuListResponse retList = user02Service.selectAuthMenuList(AuthMenuListParam.from(reuqest, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
    
    @PostMapping("/update-auth-menu-infos")
    public ResponseEntity<?> updateAuthMenuInfo(@RequestBody List<AuthMenuInfoRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization ) {
    	
    	user02Service.updateAuthMenuInfo(AuthMenuInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
