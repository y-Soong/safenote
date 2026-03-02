package com.prafta.web.attd.attd02.controller;

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

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd02.dto.HolidayListReq;
import com.prafta.web.attd.attd02.dto.HolidayListRes;
import com.prafta.web.attd.attd02.dto.HolidayReq;
import com.prafta.web.attd.attd02.service.Attd02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@RestController
@RequestMapping("/attd02")
@RequiredArgsConstructor
public class Attd02Controller { 	
	
	private final Attd02Service attd02Service;
	private final JwtUtil jwtUtil;
	
	@GetMapping("/holiday-info-lists")
	public ResponseEntity<?> getHolidays(@ModelAttribute HolidayListReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
	  	
	  	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
	  	HolidayListRes retList = attd02Service.selectHoliday(dto, tokenInfo);
			
	//  	if(retList == null) {
	//  		throw new BaimApiException("조회된 결과가 없습니다.");
	//  	}
	  	
	  	return ResponseEntity.status(HttpStatus.OK).body(retList);
	}
	
	@PostMapping("/update-holiday-infos")
	public ResponseEntity<?> updateHolidayInfo(@RequestBody HolidayReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
	  	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
	  	
	  	attd02Service.updateHolidayInfo(dto, tokenInfo);
	  	
	  	return ResponseEntity.status(HttpStatus.OK).build();
	}	
	
	
	
	
//	
//	@GetMapping("/sch-info-lists")
//    public ResponseEntity<?> getSchInfoList(@ModelAttribute SchInfoListReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
//    	
//    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
//    	SchInfoListRes retList = attd01Service.selectSchInfoList(dto, tokenInfo);
//		
////    	if(retList == null) {
////    		throw new BaimApiException("조회된 결과가 없습니다.");
////    	}
//    	
//    	return ResponseEntity.status(HttpStatus.OK).body(retList);
//    }
//	
//	@PostMapping("/update-sch-infos")
//    public ResponseEntity<?> updateSchInfo(@RequestBody SchInfoReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
//    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
//    	
//		attd01Service.updateSchInfo(dto, tokenInfo);
//    	
//    	return ResponseEntity.status(HttpStatus.OK).build();
//    }
//	
//	@GetMapping("/sch-hist-lists")
//    public ResponseEntity<?> getSchHistList(@ModelAttribute SchInfoHistReq dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
//    	
//    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
//    	
//    	SchInfoHistRes retList = attd01Service.selectSchHistList(dto, tokenInfo);
//		
////    	if(retList == null) {
////    		throw new BaimApiException("조회된 결과가 없습니다.");
////    	}
//    	
//    	return ResponseEntity.status(HttpStatus.OK).body(retList);
//    }
}
