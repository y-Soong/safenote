package com.prafta.web.tbm.tbm01.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.tbm.tbm01.application.param.TbmEduDetailParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoListParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduItemInfoListParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduItemParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduMtrlInfoParam;
//import com.prafta.web.tbm.tbm01.dto.TbmEduItemInfoReq;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduDetailRequest;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduInfoListRequest;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduInfoRequest;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduItemInfoListRequest;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduItemRequest;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduMtrlInfoRequest;
import com.prafta.web.tbm.tbm01.dto.response.TbmEduDetailResponse;
import com.prafta.web.tbm.tbm01.dto.response.TbmEduInfoListResponse;
import com.prafta.web.tbm.tbm01.dto.response.TbmEduItemInfoListResponse;
import com.prafta.web.tbm.tbm01.service.Tbm01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/tbm01")
@RequiredArgsConstructor
public class Tbm01Controller { 	
	
	private final Tbm01Service tbm01Service;
	private final JwtUtil jwtUtil;

	@GetMapping("/tbm-edu-infos")
    public ResponseEntity<?> getTbmEduInfo(@ModelAttribute TbmEduInfoListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
    	TbmEduInfoListResponse response = tbm01Service.selectTbmEduInfo(TbmEduInfoListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	@GetMapping("/tbm-edu-item-infos")
    public ResponseEntity<?> getTbmEduItemInfo(@ModelAttribute TbmEduItemInfoListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	TbmEduItemInfoListResponse response = tbm01Service.selectTbmEduItemInfo(TbmEduItemInfoListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }

	// prafta-033-A: W-03 콘텐츠 상세(묶음+세부항목+사용 TBM 이력)
	@GetMapping("/tbm-edu-detail")
    public ResponseEntity<?> getTbmEduDetail(@ModelAttribute TbmEduDetailRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	TbmEduDetailResponse response = tbm01Service.selectTbmEduDetail(TbmEduDetailParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	@PostMapping(value = "/save-tbm-edu-infos", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> saveTbmEduInfos(
			@RequestBody TbmEduInfoRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization
	) {
		
		tbm01Service.saveTbmEduInfos(TbmEduInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.ok().build();
	}
	
	@PostMapping("/delete-tbm-edu-item-infos")
	public ResponseEntity<?> deleteTbmEduItemInfo(@RequestBody List<TbmEduItemRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		tbm01Service.deleteTbmEduItemInfo(TbmEduItemParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
		return ResponseEntity.ok().build();
	}
	
	@PostMapping("/save-tbm-edus")
	public ResponseEntity<?> saveTbmEdu(@RequestBody List<TbmEduMtrlInfoRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		tbm01Service.saveTbmEdu(TbmEduMtrlInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
		return ResponseEntity.ok().build();
	}
	
	@PostMapping("/delete-tbm-edus")
	public ResponseEntity<?> deleteTbmEdu(@RequestBody List<TbmEduMtrlInfoRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		tbm01Service.deleteTbmEdu(TbmEduMtrlInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
		return ResponseEntity.ok().build();
	}
}
