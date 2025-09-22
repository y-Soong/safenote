package com.prafta.web.baim.baim01.controller;

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
import com.prafta.web.baim.baim01.dto.Baim01;
import com.prafta.web.baim.baim01.dto.Baim01ReqDto;
import com.prafta.web.baim.baim01.service.Baim01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@RestController
@RequestMapping("/baim01")
@RequiredArgsConstructor
public class Baim01Controller { 	
	
	private final Baim01Service baim01Service;
	private final JwtUtil jwtUtil;

    @PostMapping("/getSiteInfoList")
    public ResponseEntity<?> getSiteInfoList(@RequestBody Baim01ReqDto dto) {
		List<Map<String, Object>> retList = baim01Service.selectSiteInfoList(dto);
		
    	if(retList == null) {
    		throw new LoginFailException("조회된 결과가 없습니다.");
    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
    
    @PostMapping("/updateSiteInfo")
    public ResponseEntity<?> updateSiteInfo(@RequestBody List<Baim01> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	baim01Service.updateSiteInfo(dtoList, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
