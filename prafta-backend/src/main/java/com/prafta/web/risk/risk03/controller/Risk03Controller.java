package com.prafta.web.risk.risk03.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.exception.LoginFailException;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.risk.risk03.dto.RiskAssessmentsListReq;
import com.prafta.web.risk.risk03.dto.RiskAssessmentsListRes;
import com.prafta.web.risk.risk03.dto.RiskTypeListRes;
import com.prafta.web.risk.risk03.dto.SaveAssessmentReq;
import com.prafta.web.risk.risk03.service.Risk03Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@RestController
@RequestMapping("/risk03")
@RequiredArgsConstructor
public class Risk03Controller { 	
	
	private final Risk03Service risk03Service;
	private final JwtUtil jwtUtil;
	
	@GetMapping("/risk-type-info-lists")
	public ResponseEntity<?> getRiskTypeInfoList(@RequestHeader(value = "Authorization", required = false) String authorization) {
  	
		Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		RiskTypeListRes retList = risk03Service.selectRiskTypeInfoList(tokenInfo);
		
//		if(retList == null) {
//			throw new LoginFailException("조회된 결과가 없습니다.");
//		}
  	
		return ResponseEntity.status(HttpStatus.OK).body(retList);
	}
	
	@GetMapping("/risk-assessment-lists")
	public ResponseEntity<?> getRiskAssessmentsLists(@ModelAttribute RiskAssessmentsListReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
  	
		Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		RiskAssessmentsListRes retList = risk03Service.selectRiskAssessmentsLists(dto, tokenInfo);
		
//		if(retList == null) {
//			throw new LoginFailException("조회된 결과가 없습니다.");
//		}
  	
		return ResponseEntity.status(HttpStatus.OK).body(retList);
	}
	
	@PostMapping(value = "/save-assessments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> saveAssessment(@ModelAttribute SaveAssessmentReq request, @RequestPart(value = "item", required = false) MultipartFile file, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	risk03Service.saveAssessment(request, file, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
