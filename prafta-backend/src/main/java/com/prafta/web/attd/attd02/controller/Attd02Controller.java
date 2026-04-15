package com.prafta.web.attd.attd02.controller;

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
import com.prafta.web.attd.attd02.application.param.HolidayListParam;
import com.prafta.web.attd.attd02.application.param.HolidayParam;
import com.prafta.web.attd.attd02.dto.request.HolidayListRequest;
import com.prafta.web.attd.attd02.dto.request.HolidayRequest;
import com.prafta.web.attd.attd02.dto.response.HolidayListResponse;
import com.prafta.web.attd.attd02.service.Attd02Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/attd02")
@RequiredArgsConstructor
public class Attd02Controller { 	
	
	private final Attd02Service attd02Service;
	private final JwtUtil jwtUtil;
	
	@GetMapping("/holiday-info-lists")
	public ResponseEntity<?> getHolidays(@ModelAttribute HolidayListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
	  	
	  	HolidayListResponse respnse = attd02Service.selectHoliday(HolidayListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
			
	  	return ResponseEntity.status(HttpStatus.OK).body(respnse);
	}
	
	@PostMapping("/update-holiday-infos")
	public ResponseEntity<?> updateHolidayInfo(@Valid @RequestBody HolidayRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
	  	
	  	attd02Service.updateHolidayInfo(HolidayParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
	  	
	  	return ResponseEntity.status(HttpStatus.OK).build();
	}	
}
