package com.prafta.web.chkLst.chkLst01.controller;

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
import com.prafta.web.baim.baim02.dto.Baim02;
import com.prafta.web.chkLst.chkLst01.dto.ChkLst01;
import com.prafta.web.chkLst.chkLst01.dto.ChkLst01ReqDto;
import com.prafta.web.chkLst.chkLst01.service.ChkLst01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@RestController
@RequestMapping("/chkLst01")
@RequiredArgsConstructor
public class ChkLst01Controller { 	
	
	private final ChkLst01Service chkLst01Service;
	private final JwtUtil jwtUtil;

	@PostMapping("/getChkptList")
    public ResponseEntity<?> getChkptList(@RequestBody ChkLst01ReqDto dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
		List<ChkLst01> retList = chkLst01Service.selectChkptList(dto, tokenInfo);
		
    	if(retList == null) {
    		throw new LoginFailException("조회된 결과가 없습니다.");
    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
	
	@PostMapping("/updateChkptList")
    public ResponseEntity<?> updateChkptList(@RequestBody List<ChkLst01> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization ) {
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	chkLst01Service.updateChkptList(dtoList, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
	
	@PostMapping("/deleteChkptList")
    public ResponseEntity<?> deleteChkptList(@RequestBody List<ChkLst01> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization ) {
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	chkLst01Service.deleteChkptList(dtoList, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
