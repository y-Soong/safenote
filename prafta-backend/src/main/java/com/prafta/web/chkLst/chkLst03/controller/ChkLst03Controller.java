package com.prafta.web.chkLst.chkLst03.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.chkLst.chkLst03.application.param.InspectResultDetailParam;
import com.prafta.web.chkLst.chkLst03.application.param.InspectResultParam;
import com.prafta.web.chkLst.chkLst03.dto.request.InspectResultDetailRequest;
import com.prafta.web.chkLst.chkLst03.dto.request.InspectResultRequest;
import com.prafta.web.chkLst.chkLst03.dto.response.InspectResultDetailResponse;
import com.prafta.web.chkLst.chkLst03.dto.response.InspectResultResponse;
import com.prafta.web.chkLst.chkLst03.service.ChkLst03Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/chkLst03")
@RequiredArgsConstructor
public class ChkLst03Controller { 	
	
	private final ChkLst03Service chkLst03Service;
	private final JwtUtil jwtUtil;
	
	@GetMapping("/inspect-results")
	public ResponseEntity<?> getChkptInspectItemList(@ModelAttribute InspectResultRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		InspectResultResponse response = chkLst03Service.getChkptInspectItemList(InspectResultParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@GetMapping("/inspect-result-details")
	public ResponseEntity<?> getChkptInspectAnswerList(@ModelAttribute InspectResultDetailRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		InspectResultDetailResponse response = chkLst03Service.getChkptInspectAnswerList(InspectResultDetailParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}
