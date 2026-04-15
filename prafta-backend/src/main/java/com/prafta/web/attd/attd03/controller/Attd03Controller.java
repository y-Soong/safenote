package com.prafta.web.attd.attd03.controller;

import java.util.Map;

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
import com.prafta.web.attd.attd03.application.param.LeaveTypeListParam;
import com.prafta.web.attd.attd03.application.param.LeaveTypeParam;
import com.prafta.web.attd.attd03.dto.request.LeaveTypeListRequest;
import com.prafta.web.attd.attd03.dto.request.LeaveTypeRequest;
import com.prafta.web.attd.attd03.dto.response.LeaveTypeListResponse;
import com.prafta.web.attd.attd03.service.Attd03Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/attd03")
@RequiredArgsConstructor
public class Attd03Controller { 	
	
	private final Attd03Service attd03Service;
	private final JwtUtil jwtUtil;
	
	@PostMapping("/update-leave-types")
	public ResponseEntity<?> updateLeaveType(@Valid @RequestBody LeaveTypeRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
	  	
	  	attd03Service.updateLeaveType(LeaveTypeParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
	  	
	  	return ResponseEntity.status(HttpStatus.OK).build();
	}	
	
	@GetMapping("/leave-type-lists")
	public ResponseEntity<?> getLeaves(@ModelAttribute LeaveTypeListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
	  	
	  	LeaveTypeListResponse retList = attd03Service.getLeaves(LeaveTypeListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
			
	  	return ResponseEntity.status(HttpStatus.OK).body(retList);
	}
}
