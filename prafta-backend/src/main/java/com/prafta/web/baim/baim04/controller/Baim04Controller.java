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

import com.prafta.common.security.JwtUtil;
import com.prafta.web.baim.baim04.application.param.DailyUserLinkPoliciesParam;
import com.prafta.web.baim.baim04.application.param.LinkPoliciesParam;
import com.prafta.web.baim.baim04.dto.request.DailyUserLinkPoliciesRequest;
import com.prafta.web.baim.baim04.dto.request.LinkPoliciesRequest;
import com.prafta.web.baim.baim04.dto.response.DailyUserLinkPoliciesResponse;
import com.prafta.web.baim.baim04.service.Baim04Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/baim04")
@RequiredArgsConstructor
public class Baim04Controller { 	
	
	private final Baim04Service baim04Service;
	private final JwtUtil jwtUtil;

	@GetMapping("/daily-user-link-policies")
    public ResponseEntity<?> getDailyUserLinkPolicyList(@ModelAttribute DailyUserLinkPoliciesRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	DailyUserLinkPoliciesResponse response = baim04Service.selectDailyUserLinkPolicyList(DailyUserLinkPoliciesParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	@PostMapping("/save-daily-user-link-policies")
	public ResponseEntity<?> saveDailyUserLinkPolicy(@RequestBody List<LinkPoliciesRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		baim04Service.saveDailyUserLinkPolicy(LinkPoliciesParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@PostMapping("/delete-daily-user-link-policies")
	public ResponseEntity<?> deleteDailyUserLinkPolicy(@RequestBody List<LinkPoliciesRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		baim04Service.deleteDailyUserLinkPolicy(LinkPoliciesParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
}
