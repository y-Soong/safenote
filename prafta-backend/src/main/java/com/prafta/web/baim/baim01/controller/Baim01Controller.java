package com.prafta.web.baim.baim01.controller;

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
import com.prafta.common.exception.BaimApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.baim.baim01.dto.SiteInfoListReq;
import com.prafta.web.baim.baim01.dto.SiteInfoListRes;
import com.prafta.web.baim.baim01.dto.SiteInfoReq;
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

//    @PostMapping("/getSiteInfoList")
	@GetMapping("/site-info-lists")
    public ResponseEntity<?> getSiteInfoList(@ModelAttribute SiteInfoListReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
		Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		
		SiteInfoListRes retList = baim01Service.selectSiteInfoList(dto, tokenInfo);
		
//    	if(retList == null) {
//    		throw new BaimApiException("조회된 결과가 없습니다.");
//    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
    
    @PostMapping("/save-site-infos")
    public ResponseEntity<?> saveSiteInfo(@RequestBody List<SiteInfoReq> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	baim01Service.saveSiteInfo(dtoList, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
