package com.prafta.web.chkLst.chkLst01.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.chkLst.chkLst01.application.param.ChkptInfoParam;
import com.prafta.web.chkLst.chkLst01.application.param.ChkptListParam;
import com.prafta.web.chkLst.chkLst01.dto.request.ChkptInfoRequest;
import com.prafta.web.chkLst.chkLst01.dto.request.ChkptListRequest;
import com.prafta.web.chkLst.chkLst01.dto.response.ChkptListResponse;
import com.prafta.web.chkLst.chkLst01.result.ChkptResult;
import com.prafta.web.chkLst.chkLst01.service.ChkLst01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping("/chkLst01")
@RequiredArgsConstructor
public class ChkLst01Controller {
	
	private final ChkLst01Service chkLst01Service;
	private final JwtUtil jwtUtil;

	@GetMapping("/chkpt-lists")
    public ResponseEntity<?> getChkptList(@ModelAttribute ChkptListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		ChkptListResponse response = chkLst01Service.selectChkptList(ChkptListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	@PostMapping("/update-chkpt-lists")
    public ResponseEntity<?> updateChkptList(@RequestBody @Valid List<@Valid ChkptInfoRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization ) {
		
		System.out.println(request.toString());
    	
    	chkLst01Service.updateChkptList(ChkptInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
	
	@PostMapping("/deleteChkptList")
    public ResponseEntity<?> deleteChkptList(@RequestBody List<ChkptInfoRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization ) {
    	
    	chkLst01Service.deleteChkptList(ChkptInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
