package com.prafta.web.user.user02.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.exception.LoginFailException;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.user.user02.dto.User02;
import com.prafta.web.user.user02.dto.User02ReqDto;
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

    @PostMapping("/getAuthMenuList")
    public ResponseEntity<?> getAuthMenuList(@RequestBody User02ReqDto dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		List<Map<String, Object>> retList = user02Service.selectAuthMenuList(dto, tokenInfo);
		
    	if(retList == null) {
    		throw new LoginFailException("조회된 결과가 없습니다.");
    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
    
    @PostMapping("/updateAuthMenuInfo")
    public ResponseEntity<?> updateAuthMenuInfo(@RequestBody List<User02> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization ) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	user02Service.updateAuthMenuInfo(dtoList, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
