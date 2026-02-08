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

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.user.user03.dto.SiteInfoListReq;
import com.prafta.web.user.user03.dto.SiteInfoListRes;
import com.prafta.web.user.user03.dto.User03;
import com.prafta.web.user.user03.service.User03Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@RestController
@RequestMapping("/user03")
@RequiredArgsConstructor
public class User03Controller { 	
	
	private final User03Service user03Service;
	private final JwtUtil jwtUtil;

//    @PostMapping("/getSiteInfoSearch")
    @GetMapping("/site-info-lists")
    public ResponseEntity<?> getSiteInfoList(@ModelAttribute SiteInfoListReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	SiteInfoListRes retDto = user03Service.selectSiteInfoSearch(dto, tokenInfo);
		
//    	if(retList == null) {
//    		throw new LoginFailException("조회된 결과가 없습니다.");
//    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retDto);
    }
    
    @PostMapping("/updateUserSiteAuth")
    public ResponseEntity<?> updateUserSiteAuth(@RequestBody List<User03> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization ) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	user03Service.updateUserSiteAuth(dtoList, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
