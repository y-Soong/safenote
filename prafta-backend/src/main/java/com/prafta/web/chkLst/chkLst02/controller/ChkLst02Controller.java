package com.prafta.web.chkLst.chkLst02.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.exception.LoginFailException;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.chkLst.chkLst02.dto.ChkLst02;
import com.prafta.web.chkLst.chkLst02.dto.ChkLst02ReqDto;
import com.prafta.web.chkLst.chkLst02.service.ChkLst02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@RestController
@RequestMapping("/chkLst02")
@RequiredArgsConstructor
public class ChkLst02Controller { 	
	
	private final ChkLst02Service chkLst02Service;
	private final JwtUtil jwtUtil;

	@PostMapping("/getChkptInspectItemList")
    public ResponseEntity<?> getChkptInspectItemList(@RequestBody ChkLst02ReqDto dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		List<ChkLst02> retList = chkLst02Service.selectChkptInspectItemList(dto, tokenInfo);
		
    	if(retList == null) {
    		throw new LoginFailException("조회된 결과가 없습니다.");
    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
	
	@PostMapping("/updateChkptInspectItemList")
    public ResponseEntity<?> updateChkptInspectItemList(@RequestBody List<ChkLst02> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization ) {
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	chkLst02Service.updateChkptInspectItemList(dtoList, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
	
	@PostMapping("/deleteChkptInspectItemList")
    public ResponseEntity<?> deleteChkptInspectItemList(@RequestBody List<ChkLst02> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization ) {
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	chkLst02Service.deleteChkptInspectItemList(dtoList, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
