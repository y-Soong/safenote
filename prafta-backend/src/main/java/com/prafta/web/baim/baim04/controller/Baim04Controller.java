package com.prafta.web.baim.baim04.controller;

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
import com.prafta.web.baim.baim04.dto.DailyUserLinkPoliciesReq;
import com.prafta.web.baim.baim04.dto.DailyUserLinkPoliciesRes;
import com.prafta.web.baim.baim04.dto.LinkPoliciesReq;
import com.prafta.web.baim.baim04.service.Baim04Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@RestController
@RequestMapping("/baim04")
@RequiredArgsConstructor
public class Baim04Controller { 	
	
	private final Baim04Service baim04Service;
	private final JwtUtil jwtUtil;

	@GetMapping("/daily-user-link-policies")
    public ResponseEntity<?> getDailyUserLinkPolicyList(@ModelAttribute DailyUserLinkPoliciesReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	DailyUserLinkPoliciesRes retList = baim04Service.selectDailyUserLinkPolicyList(dto, tokenInfo);
		
//    	if(retList == null) {
//    		throw new BaimApiException("조회된 결과가 없습니다.");
//    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
	
	@PostMapping("/save-daily-user-link-policies")
	public ResponseEntity<?> saveDailyUserLinkPolicy(@RequestBody List<LinkPoliciesReq> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization) {
		Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		
		baim04Service.saveDailyUserLinkPolicy(dtoList, tokenInfo);

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@PostMapping("/delete-daily-user-link-policies")
	public ResponseEntity<?> deleteDailyUserLinkPolicy(@RequestBody List<LinkPoliciesReq> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization) {
		Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		
		baim04Service.deleteDailyUserLinkPolicy(dtoList, tokenInfo);

		return ResponseEntity.status(HttpStatus.OK).build();
	}
}
