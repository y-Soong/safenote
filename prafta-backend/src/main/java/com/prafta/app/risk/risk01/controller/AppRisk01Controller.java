package com.prafta.app.risk.risk01.controller;

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

import com.prafta.app.risk.risk01.dto.RiskAssessmentReq;
import com.prafta.app.risk.risk01.dto.RiskInfoReq;
import com.prafta.app.risk.risk01.dto.RiskInfoRes;
import com.prafta.app.risk.risk01.service.AppRisk01Service;
import com.prafta.common.annotation.NoAuth;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.exception.CmmApiException;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@Slf4j
@NoAuth
@RestController
@RequestMapping("/risk01")
@RequiredArgsConstructor // 롬복이 final 필드로 생성자 자동 생성
public class AppRisk01Controller {
	
	private final AppRisk01Service appRisk01Service;
	
	private final FileService fileService;
	
	private final JwtUtil jwtUtil;
	
	/* 위험성평가 구분, 분류, 발생상황 조회 */
	@GetMapping("/risk-type-infos")
	public ResponseEntity<?> getRiskTypeInfo(@ModelAttribute RiskInfoReq request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		
		RiskInfoRes retDto = appRisk01Service.selectRiskTypeInfo(request, tokenInfo);
		
		if(retDto == null) {
    		throw new CmmApiException("조회결과가 없습니다.");
    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retDto);
	}
	
	@PostMapping(value = "/save-risk-assessments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveRiskAssessments(@ModelAttribute RiskAssessmentReq request, @RequestPart(value = "item", required = false) MultipartFile file, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	appRisk01Service.saveRiskAssessments(request, file, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
	
//	/* 체크리스트 정보조회 */
//    @GetMapping("/checklist-infos")
//    public ResponseEntity<?> getChkLstInfo(@ModelAttribute ChecklistInfoReq request, @RequestHeader(value = "Authorization", required = false) String authorization) {
//    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
//    	
//    	System.out.print("request ###");
//    	System.out.println(request);
//    	
//    	ChecklistInfoRes retDto = appChkLst01Service.selectChkLstInfo(request, tokenInfo);
//    	
//    	if(retDto == null) {
//    		throw new CmmApiException("조회결과가 없습니다.");
//    	}
//    	
//    	return ResponseEntity.status(HttpStatus.OK).body(retDto);
//    }
//    
//    @PostMapping(value = "/save-inspect-result", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<?> saveInspectResult(@ModelAttribute SaveInspectResultReq request, @RequestParam(required = false) Map<String, MultipartFile> file, @RequestHeader(value = "Authorization", required = false) String authorization) {
//    	
//    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
//    	
//    	appChkLst01Service.saveInspectResult(request, file, tokenInfo);
//    	
//    	return ResponseEntity.status(HttpStatus.OK).build();
//    }
}
