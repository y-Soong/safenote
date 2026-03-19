package com.prafta.web.attd.attd01.controller;

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
import com.prafta.web.attd.attd01.dto.SchInfoHistReq;
import com.prafta.web.attd.attd01.dto.SchInfoHistRes;
import com.prafta.web.attd.attd01.dto.SchInfoListReq;
import com.prafta.web.attd.attd01.dto.SchInfoListRes;
import com.prafta.web.attd.attd01.dto.SchInfoReq;
import com.prafta.web.attd.attd01.dto.ShiftSchDetailReq;
import com.prafta.web.attd.attd01.dto.ShiftSchDetailRes;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoListReq;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoListRes;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoReq;
import com.prafta.web.attd.attd01.service.Attd01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@RestController
@RequestMapping("/attd01")
@RequiredArgsConstructor
public class Attd01Controller { 	
	
	private final Attd01Service attd01Service;
	private final JwtUtil jwtUtil;
	
	@GetMapping("/sch-info-lists")
    public ResponseEntity<?> getSchInfoList(@ModelAttribute SchInfoListReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	SchInfoListRes retList = attd01Service.selectSchInfoList(dto, tokenInfo);
		
//    	if(retList == null) {
//    		throw new BaimApiException("조회된 결과가 없습니다.");
//    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
	
	@PostMapping("/update-sch-infos")
    public ResponseEntity<?> updateSchInfo(@RequestBody SchInfoReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
		attd01Service.updateSchInfo(dto, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
	
	@GetMapping("/sch-hist-lists")
    public ResponseEntity<?> getSchHistList(@ModelAttribute SchInfoHistReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	SchInfoHistRes retList = attd01Service.selectSchHistList(dto, tokenInfo);
		
//    	if(retList == null) {
//    		throw new BaimApiException("조회된 결과가 없습니다.");
//    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
	
	@PostMapping("/update-shift-sch-infos")
    public ResponseEntity<?> updateShiftSchInfo(@RequestBody ShiftSchInfoReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
		attd01Service.updateShiftSchInfo(dto, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
	
	@GetMapping("/shift-sch-info-lists")
    public ResponseEntity<?> getShiftSchInfoList(@ModelAttribute ShiftSchInfoListReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	ShiftSchInfoListRes retList = attd01Service.selectShiftSchInfoList(dto, tokenInfo);
		
//    	if(retList == null) {
//    		throw new BaimApiException("조회된 결과가 없습니다.");
//    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
	
	@GetMapping("/shift-sch-details")
    public ResponseEntity<?> getShiftSchDetail(@ModelAttribute ShiftSchDetailReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	ShiftSchDetailRes retList = attd01Service.selectShiftSchDetail(dto, tokenInfo);
		
//    	if(retList == null) {
//    		throw new BaimApiException("조회된 결과가 없습니다.");
//    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
}
