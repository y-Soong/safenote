package com.prafta.web.baim.baim02.controller;

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
import com.prafta.web.baim.baim02.application.param.CompCmmCodeDListParam;
import com.prafta.web.baim.baim02.application.param.CompCmmCodeDParam;
import com.prafta.web.baim.baim02.application.param.CompCmmCodeMListParam;
import com.prafta.web.baim.baim02.dto.request.CompCmmCodeDListRequest;
import com.prafta.web.baim.baim02.dto.request.CompCmmCodeDRequest;
import com.prafta.web.baim.baim02.dto.request.CompCmmCodeMListRequest;
import com.prafta.web.baim.baim02.dto.response.CompCmmCodeDListResponse;
import com.prafta.web.baim.baim02.dto.response.CompCmmCodeMListResponse;
import com.prafta.web.baim.baim02.service.Baim02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/baim02")
@RequiredArgsConstructor
public class Baim02Controller {
	
	private final Baim02Service baim02Service;
	private final JwtUtil jwtUtil;

    @GetMapping("/comp-cmm-code-m-list")
    public ResponseEntity<?> getCompCmmCodeMList(@ModelAttribute CompCmmCodeMListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	    	CompCmmCodeMListResponse response = baim02Service.selectCompCmmCodeMList(CompCmmCodeMListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @GetMapping("/comp-cmm-code-d-list")
    public ResponseEntity<?> getCompCmmCodeDList(@ModelAttribute CompCmmCodeDListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	CompCmmCodeDListResponse response = baim02Service.selectCompCmmCodeDList(CompCmmCodeDListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @PostMapping("/update-cmm-code-detail-info")
    public ResponseEntity<?> updateCmmCodeDetailInfo(@RequestBody List<CompCmmCodeDRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization ) {
    	
    	baim02Service.updateCmmCodeDetailInfo(CompCmmCodeDParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
    
    @PostMapping("/deleteCmmCodeDetailInfo")
    public ResponseEntity<?> deleteCmmCodeDetailInfo(@RequestBody List<CompCmmCodeDRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization ) {
    	
    	baim02Service.deleteCmmCodeDetailInfo(CompCmmCodeDParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
