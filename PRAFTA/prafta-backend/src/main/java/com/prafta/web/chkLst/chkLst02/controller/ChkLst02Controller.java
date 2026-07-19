package com.prafta.web.chkLst.chkLst02.controller;

import java.util.List;

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
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemHistListParam;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemListParam;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemParam;
import com.prafta.web.chkLst.chkLst02.application.param.CopyChkptInspectItemParam;
import com.prafta.web.chkLst.chkLst02.dto.request.ChkptInspectItemHistListRequest;
import com.prafta.web.chkLst.chkLst02.dto.request.ChkptInspectItemListRequest;
import com.prafta.web.chkLst.chkLst02.dto.request.ChkptInspectItemRequest;
import com.prafta.web.chkLst.chkLst02.dto.request.CopyChkptInspectItemRequest;
import com.prafta.web.chkLst.chkLst02.dto.response.ChkptInspectItemHistListResponse;
import com.prafta.web.chkLst.chkLst02.dto.response.ChkptInspectItemListResponse;
import com.prafta.web.chkLst.chkLst02.service.ChkLst02Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/chkLst02")
@RequiredArgsConstructor
@Validated
public class ChkLst02Controller { 	
	
	private final ChkLst02Service chkLst02Service;
	private final JwtUtil jwtUtil;

	@GetMapping("/chkpt-inspect-item-lists")
    public ResponseEntity<?> getChkptInspectItemList(@ModelAttribute ChkptInspectItemListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		ChkptInspectItemListResponse response = chkLst02Service.selectChkptInspectItemList(ChkptInspectItemListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	@PostMapping("/update-chkpt-inspect-items")
    public ResponseEntity<?> updateChkptInspectItemList(@Valid @RequestBody List<ChkptInspectItemRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization ) {
    	
    	chkLst02Service.updateChkptInspectItemList(ChkptInspectItemParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
	
	@PostMapping("/delete-chkpt-inspect-items")
    public ResponseEntity<?> deleteChkptInspectItemList(@Valid @RequestBody List<ChkptInspectItemRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization ) {

    	chkLst02Service.deleteChkptInspectItemList(ChkptInspectItemParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).build();
    }

	/** PRAFTA-SUBCON-T0-04: 타 사업장 점검문항 가져오기(선택 복사 — Baim06 copy-site-nodes 패턴 준용) */
	@PostMapping("/copy-chkpt-inspect-items")
    public ResponseEntity<?> copyChkptInspectItemList(@Valid @RequestBody CopyChkptInspectItemRequest request, @RequestHeader(value = "Authorization", required = false) String authorization ) {

    	chkLst02Service.copyChkptInspectItemList(CopyChkptInspectItemParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).build();
    }

	/** 문항 변경이력 조회(문항관리 이력 팝업) */
	@GetMapping("/chkpt-inspect-item-hists")
    public ResponseEntity<?> getChkptInspectItemHistList(@ModelAttribute ChkptInspectItemHistListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {

		ChkptInspectItemHistListResponse response = chkLst02Service.selectChkptInspectItemHistList(ChkptInspectItemHistListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
