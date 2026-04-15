package com.prafta.web.baim.baim05.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.baim.baim05.application.param.DailyUserSlotListParam;
import com.prafta.web.baim.baim05.dto.request.DailyUserSlotListRequest;
import com.prafta.web.baim.baim05.dto.response.DailyUserSlotListResponse;
import com.prafta.web.baim.baim05.service.Baim05Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/baim05")
@RequiredArgsConstructor
public class Baim05Controller { 	
	
	private final Baim05Service baim05Service;
	private final JwtUtil jwtUtil;
	
	@GetMapping("/daily-user-slot-lists")
    public ResponseEntity<?> getDailyUserSlotList(@ModelAttribute DailyUserSlotListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	DailyUserSlotListResponse response = baim05Service.selectDailyUserSlotList(DailyUserSlotListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
