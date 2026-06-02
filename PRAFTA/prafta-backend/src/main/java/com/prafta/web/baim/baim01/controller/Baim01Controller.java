package com.prafta.web.baim.baim01.controller;

import java.util.List;

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
import com.prafta.web.baim.baim01.application.param.SiteInfoListParam;
import com.prafta.web.baim.baim01.application.param.SiteInfoParam;
import com.prafta.web.baim.baim01.dto.request.SiteInfoListRequest;
import com.prafta.web.baim.baim01.dto.request.SiteInfoRequest;
import com.prafta.web.baim.baim01.dto.response.SiteInfoListResponse;
import com.prafta.web.baim.baim01.service.Baim01Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/baim01")
@RequiredArgsConstructor
public class Baim01Controller { 	
	
	private final Baim01Service baim01Service;
	private final JwtUtil jwtUtil;

	@GetMapping("/site-info-lists")
    public ResponseEntity<?> getSiteInfoList(@ModelAttribute SiteInfoListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		SiteInfoListResponse response = baim01Service.selectSiteInfoList(SiteInfoListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @PostMapping("/save-site-infos")
    public ResponseEntity<?> saveSiteInfo(@Valid @RequestBody List<@Valid SiteInfoRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	baim01Service.saveSiteInfo(SiteInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
