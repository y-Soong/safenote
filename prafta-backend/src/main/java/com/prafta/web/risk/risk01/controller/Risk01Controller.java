package com.prafta.web.risk.risk01.controller;

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
import com.prafta.common.exception.LoginFailException;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.risk.risk01.dto.RiskHazardListReq;
import com.prafta.web.risk.risk01.dto.RiskHazardListRes;
import com.prafta.web.risk.risk01.dto.RiskHazardListReq;
import com.prafta.web.risk.risk01.dto.RiskHazardListRes;
import com.prafta.web.risk.risk01.dto.RiskHazardReq;
import com.prafta.web.risk.risk01.dto.RiskTypeListReq;
import com.prafta.web.risk.risk01.dto.RiskTypeListRes;
import com.prafta.web.risk.risk01.dto.RiskTypeReq;
import com.prafta.web.risk.risk01.service.Risk01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@RestController
@RequestMapping("/risk01")
@RequiredArgsConstructor
public class Risk01Controller { 	
	
	private final Risk01Service risk01Service;
	private final JwtUtil jwtUtil;
	
	@GetMapping("/risk-type-lists")
    public ResponseEntity<?> getRiskTypeList(@ModelAttribute RiskTypeListReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	RiskTypeListRes retList = risk01Service.selectRiskTypeList(dto, tokenInfo);
		
//    	if(retList == null) {
//    		throw new LoginFailException("조회된 결과가 없습니다.");
//    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
	
	@PostMapping("/update-risk-types")
	public ResponseEntity<?> updateRistType(@RequestBody List<RiskTypeReq> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization) {
		Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		
		risk01Service.updateRistType(dtoList, tokenInfo);

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@PostMapping("/delete-risk-types")
	public ResponseEntity<?> deleteRistType(@RequestBody List<RiskTypeReq> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization) {
		Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		
		risk01Service.deleteRistType(dtoList, tokenInfo);

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@GetMapping("/risk-hazard-lists")
    public ResponseEntity<?> getRiskHazardList(@ModelAttribute RiskHazardListReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	RiskHazardListRes retList = risk01Service.selectRiskHazardList(dto, tokenInfo);
		
//    	if(retList == null) {
//    		throw new LoginFailException("조회된 결과가 없습니다.");
//    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
	
	@PostMapping("/update-risk-hazards")
	public ResponseEntity<?> updateRiskHazard(@RequestBody List<RiskHazardReq> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization) {
		Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		
		risk01Service.updateRiskHazard(dtoList, tokenInfo);

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@PostMapping("/delete-risk-hazards")
	public ResponseEntity<?> deleteRiskHazard(@RequestBody List<RiskHazardReq> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization) {
		Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		
		risk01Service.deleteRiskHazard(dtoList, tokenInfo);

		return ResponseEntity.status(HttpStatus.OK).build();
	}
}
