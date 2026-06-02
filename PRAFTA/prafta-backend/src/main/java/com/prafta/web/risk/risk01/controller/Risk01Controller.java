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
import com.prafta.common.security.JwtUtil;
import com.prafta.web.risk.risk01.application.param.RiskHazardListParam;
import com.prafta.web.risk.risk01.application.param.RiskHazardParam;
import com.prafta.web.risk.risk01.application.param.RiskTypeListParam;
import com.prafta.web.risk.risk01.application.param.RiskTypeParam;
import com.prafta.web.risk.risk01.dto.request.RiskHazardListRequest;
import com.prafta.web.risk.risk01.dto.request.RiskHazardRequest;
import com.prafta.web.risk.risk01.dto.request.RiskTypeListRequest;
import com.prafta.web.risk.risk01.dto.request.RiskTypeRequest;
import com.prafta.web.risk.risk01.dto.response.RiskHazardListResponse;
import com.prafta.web.risk.risk01.dto.response.RiskTypeListResponse;
import com.prafta.web.risk.risk01.service.Risk01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/risk01")
@RequiredArgsConstructor
public class Risk01Controller { 	
	
	private final Risk01Service risk01Service;
	private final JwtUtil jwtUtil;
	
	@GetMapping("/risk-type-lists")
    public ResponseEntity<?> getRiskTypeList(@ModelAttribute RiskTypeListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	RiskTypeListResponse response = risk01Service.selectRiskTypeList(RiskTypeListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	@PostMapping("/update-risk-types")
	public ResponseEntity<?> updateRistType(@RequestBody List<RiskTypeRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		risk01Service.updateRistType(RiskTypeParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@PostMapping("/delete-risk-types")
	public ResponseEntity<?> deleteRistType(@RequestBody List<RiskTypeRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		risk01Service.deleteRistType(RiskTypeParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@GetMapping("/risk-hazard-lists")
    public ResponseEntity<?> getRiskHazardList(@ModelAttribute RiskHazardListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	RiskHazardListResponse response = risk01Service.selectRiskHazardList(RiskHazardListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	@PostMapping("/update-risk-hazards")
	public ResponseEntity<?> updateRiskHazard(@RequestBody List<RiskHazardRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		risk01Service.updateRiskHazard(RiskHazardParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@PostMapping("/delete-risk-hazards")
	public ResponseEntity<?> deleteRiskHazard(@RequestBody List<RiskHazardRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		risk01Service.deleteRiskHazard(RiskHazardParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
}
