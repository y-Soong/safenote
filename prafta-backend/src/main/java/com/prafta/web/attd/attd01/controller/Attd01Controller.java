package com.prafta.web.attd.attd01.controller;

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

import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd01.application.param.SchInfoHistParam;
import com.prafta.web.attd.attd01.application.param.SchInfoListParam;
import com.prafta.web.attd.attd01.application.param.SchInfoParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchDetailParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoListParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam;
import com.prafta.web.attd.attd01.dto.request.SchInfoHistRequest;
import com.prafta.web.attd.attd01.dto.request.SchInfoListRequest;
import com.prafta.web.attd.attd01.dto.request.SchInfoRequest;
import com.prafta.web.attd.attd01.dto.request.ShiftSchDetailRequest;
import com.prafta.web.attd.attd01.dto.request.ShiftSchInfoListRequest;
import com.prafta.web.attd.attd01.dto.request.ShiftSchInfoRequest;
import com.prafta.web.attd.attd01.dto.response.SchInfoHistResponse;
import com.prafta.web.attd.attd01.dto.response.SchInfoListResponse;
import com.prafta.web.attd.attd01.dto.response.ShiftSchDetailResponse;
import com.prafta.web.attd.attd01.dto.response.ShiftSchInfoListResponse;
import com.prafta.web.attd.attd01.service.Attd01Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/attd01")
@RequiredArgsConstructor
@Validated
public class Attd01Controller { 	
	
	private final Attd01Service attd01Service;
	private final JwtUtil jwtUtil;
	
	@GetMapping("/sch-info-lists")
    public ResponseEntity<?> getSchInfoList(@ModelAttribute SchInfoListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	SchInfoListResponse response = attd01Service.selectSchInfoList(SchInfoListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	@PostMapping("/update-sch-infos")
    public ResponseEntity<?> updateSchInfo(@Valid @RequestBody SchInfoRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
		attd01Service.updateSchInfo(SchInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
	
	@GetMapping("/sch-hist-lists")
    public ResponseEntity<?> getSchHistList(@ModelAttribute SchInfoHistRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	SchInfoHistResponse response = attd01Service.selectSchHistList(SchInfoHistParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	@PostMapping("/update-shift-sch-infos")
    public ResponseEntity<?> updateShiftSchInfo(@RequestBody ShiftSchInfoRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		attd01Service.updateShiftSchInfo(ShiftSchInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
	
	@GetMapping("/shift-sch-info-lists")
    public ResponseEntity<?> getShiftSchInfoList(@ModelAttribute ShiftSchInfoListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	ShiftSchInfoListResponse response = attd01Service.selectShiftSchInfoList(ShiftSchInfoListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	@GetMapping("/shift-sch-details")
    public ResponseEntity<?> getShiftSchDetail(@ModelAttribute ShiftSchDetailRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	ShiftSchDetailResponse retList = attd01Service.selectShiftSchDetail(ShiftSchDetailParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
}
