package com.prafta.web.user.user03.controller;

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

import com.prafta.common.security.JwtUtil;
import com.prafta.web.user.user03.application.param.SiteInfoListParam;
import com.prafta.web.user.user03.application.param.UserSiteAuthParam;
import com.prafta.web.user.user03.dto.request.SiteInfoListRequest;
import com.prafta.web.user.user03.dto.request.UserSiteAuthRequest;
import com.prafta.web.user.user03.dto.response.SiteInfoListResponse;
import com.prafta.web.user.user03.service.User03Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/user03")
@RequiredArgsConstructor
public class User03Controller { 	
	
	private final User03Service user03Service;
	private final JwtUtil jwtUtil;

    @GetMapping("/site-info-lists")
    public ResponseEntity<?> getSiteInfoList(@ModelAttribute SiteInfoListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	SiteInfoListResponse response = user03Service.selectSiteInfoSearch(SiteInfoListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @PostMapping("/update-user-site-auth")
    public ResponseEntity<?> updateUserSiteAuth(@RequestBody List<UserSiteAuthRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization ) {
    	    	
    	user03Service.updateUserSiteAuth(UserSiteAuthParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
