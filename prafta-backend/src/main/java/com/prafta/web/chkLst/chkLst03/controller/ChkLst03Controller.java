package com.prafta.web.chkLst.chkLst03.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.chkLst.chkLst03.dto.InspectResultDetailRequest;
import com.prafta.web.chkLst.chkLst03.dto.InspectResultDetailResponse;
import com.prafta.web.chkLst.chkLst03.dto.InspectResultRequest;
import com.prafta.web.chkLst.chkLst03.dto.InspectResultResponse;
import com.prafta.web.chkLst.chkLst03.service.ChkLst03Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@RestController
@RequestMapping("/chkLst03")
@RequiredArgsConstructor
public class ChkLst03Controller { 	
	
	private final ChkLst03Service chkLst03Service;
	private final JwtUtil jwtUtil;
	
	@GetMapping("/inspect-results")
	public ResponseEntity<?> getChkptInspectItemList(@ModelAttribute InspectResultRequest dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		
		InspectResultResponse resDto = chkLst03Service.getChkptInspectItemList(dto, tokenInfo);
		
		return ResponseEntity.status(HttpStatus.OK).body(resDto);
	}
	
	@GetMapping("/inspect-result-details")
	public ResponseEntity<?> getChkptInspectAnswerList(@ModelAttribute InspectResultDetailRequest dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		
		InspectResultDetailResponse resDto = chkLst03Service.getChkptInspectAnswerList(dto, tokenInfo);
		
		return ResponseEntity.status(HttpStatus.OK).body(resDto);
	}
//	@PostMapping("/getChkptInspectItemList")
//    public ResponseEntity<?> getChkptInspectItemList(@RequestBody ChkLst02ReqDto dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
//    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
//		List<ChkLst02> retList = chkLst03Service.selectChkptInspectItemList(dto, tokenInfo);
//		
//    	if(retList == null) {
//    		throw new LoginFailException("조회된 결과가 없습니다.");
//    	}
//    	
//    	return ResponseEntity.status(HttpStatus.OK).body(retList);
//    }
//	
//	@PostMapping("/updateChkptInspectItemList")
//    public ResponseEntity<?> updateChkptInspectItemList(@RequestBody List<ChkLst02> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization ) {
//    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
//    	
//    	chkLst03Service.updateChkptInspectItemList(dtoList, tokenInfo);
//    	
//    	return ResponseEntity.status(HttpStatus.OK).build();
//    }
//	
//	@PostMapping("/deleteChkptInspectItemList")
//    public ResponseEntity<?> deleteChkptInspectItemList(@RequestBody List<ChkLst02> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization ) {
//    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
//    	
//    	chkLst03Service.deleteChkptInspectItemList(dtoList, tokenInfo);
//    	
//    	return ResponseEntity.status(HttpStatus.OK).build();
//    }
}
