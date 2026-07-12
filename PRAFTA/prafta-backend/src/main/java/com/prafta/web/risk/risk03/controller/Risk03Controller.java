package com.prafta.web.risk.risk03.controller;

import java.util.Base64;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.dto.BytesMultipartFile;
import com.prafta.common.error.risk.RiskErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.risk.risk03.application.param.AssessmentParam;
import com.prafta.web.risk.risk03.application.param.RiskAssessmentsListParam;
import com.prafta.web.risk.risk03.application.param.RiskTypeInfoListParam;
import com.prafta.web.risk.risk03.dto.request.AssessmentRequest;
import com.prafta.web.risk.risk03.dto.request.RiskAssessmentsListRequest;
import com.prafta.web.risk.risk03.dto.response.RiskAssessmentsListResponse;
import com.prafta.web.risk.risk03.dto.response.RiskTypeListResponse;
import com.prafta.web.risk.risk03.service.Risk03Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/risk03")
@RequiredArgsConstructor
public class Risk03Controller { 	
	
	private final Risk03Service risk03Service;
	private final JwtUtil jwtUtil;

	// Low-B: 업로드 파일 크기 상한(10MB) — 과대 Base64 페이로드로 인한 메모리 압박 차단
	private static final int MAX_UPLOAD_BYTES = 10 * 1024 * 1024;
	
	@GetMapping("/risk-type-info-lists")
	public ResponseEntity<?> getRiskTypeInfoList(@RequestHeader(value = "Authorization", required = false) String authorization) {
  	
		RiskTypeListResponse response = risk03Service.selectRiskTypeInfoList(RiskTypeInfoListParam.from(jwtUtil.getAllClaimsAsMap(authorization)));
		
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@GetMapping("/risk-assessment-lists")
	public ResponseEntity<?> getRiskAssessmentsLists(@ModelAttribute RiskAssessmentsListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
  	
		RiskAssessmentsListResponse response = risk03Service.selectRiskAssessmentsLists(RiskAssessmentsListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@PostMapping(value = "/save-assessments", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> saveAssessment(@RequestBody AssessmentRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {

		MultipartFile file = null;
		if (StringUtils.hasText(request.getItemBase64())) {
			byte[] bytes = Base64.getDecoder().decode(request.getItemBase64().trim());
			// Low-B: 디코딩된 실제 바이트 기준 크기 상한 검사(저장 전 차단)
			if (bytes.length > MAX_UPLOAD_BYTES) {
				throw new ApiException(RiskErrorCode.RISK_400_003);
			}
			String fileName = StringUtils.hasText(request.getItemOriginalFilename())
					? request.getItemOriginalFilename()
					: "upload.bin";
			file = new BytesMultipartFile("item", fileName, null, bytes);
		}

		risk03Service.saveAssessment(AssessmentParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)), file);

		return ResponseEntity.status(HttpStatus.OK).build();
	}
}
