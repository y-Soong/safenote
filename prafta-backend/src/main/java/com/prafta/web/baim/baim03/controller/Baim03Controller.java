package com.prafta.web.baim.baim03.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.baim.baim03.application.param.TermsDetailInfoListParam;
import com.prafta.web.baim.baim03.application.param.TermsInfoListParam;
import com.prafta.web.baim.baim03.application.param.TermsInfoParam;
import com.prafta.web.baim.baim03.application.param.TermsListParam;
import com.prafta.web.baim.baim03.dto.request.TermsDetailInfoListRequest;
import com.prafta.web.baim.baim03.dto.request.TermsInfoListRequest;
import com.prafta.web.baim.baim03.dto.request.TermsInfoRequest;
import com.prafta.web.baim.baim03.dto.response.TermsDetailInfoListResponse;
import com.prafta.web.baim.baim03.dto.response.TermsInfoListResponse;
import com.prafta.web.baim.baim03.service.Baim03Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/baim03")
@RequiredArgsConstructor
public class Baim03Controller { 	
	
	private final Baim03Service baim03Service;
	private final JwtUtil jwtUtil;

	@GetMapping("/terms-info-lists")
    public ResponseEntity<?> getTermsList(@ModelAttribute TermsInfoListRequest request) {
    	
    	TermsInfoListResponse response = baim03Service.selectTermsList(TermsInfoListParam.from(request));

    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
	@GetMapping("/terms-detail-info-list")
    public ResponseEntity<?> getTermsDList(@ModelAttribute TermsDetailInfoListRequest request) {
		
    	TermsDetailInfoListResponse response = baim03Service.selectTermsDList(TermsDetailInfoListParam.from(request));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/update-terms-info")
    public ResponseEntity<?> updateTermsInfo(@RequestBody TermsInfoRequest request, @RequestHeader(value = "Authorization", required = false) String authorization ) {
    	
    	baim03Service.updateTermsInfo(TermsInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
    
    /* 작업하다 만듯 .. 이용약관 쪽 삭제가 필요할 때 마저 진행하자 */
    @PostMapping("/deleteCmmCodeDetailInfo")
    public ResponseEntity<?> deleteCmmCodeDetailInfo(@RequestBody List<TermsInfoRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization ) {
//    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	baim03Service.deleteCmmCodeDetailInfo(TermsListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
