package com.prafta.app.chkLst.chkLst01.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoReq;
import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoRes;
import com.prafta.app.chkLst.chkLst01.dto.SaveInspectResultReq;
import com.prafta.app.chkLst.chkLst01.service.AppChkLst01Service;
import com.prafta.common.annotation.NoAuth;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.exception.CmmApiException;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@RestController
@RequestMapping("/chkLst01")
@RequiredArgsConstructor // 롬복이 final 필드로 생성자 자동 생성
public class AppChkLst01Controller {
	
	private final AppChkLst01Service appChkLst01Service;
	
	private final FileService fileService;
	
	private final JwtUtil jwtUtil;
	
	/* 체크리스트 정보조회 */
    @GetMapping("/checklist-infos")
    public ResponseEntity<?> getChkLstInfo(@ModelAttribute ChecklistInfoReq request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	System.out.print("request ###");
    	System.out.println(request);
    	
    	ChecklistInfoRes retDto = appChkLst01Service.selectChkLstInfo(request, tokenInfo);
    	
    	if(retDto == null) {
    		throw new CmmApiException("조회결과가 없습니다.");
    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retDto);
    }
    
    @PostMapping(value = "/save-inspect-result", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveInspectResult(@ModelAttribute SaveInspectResultReq request, @RequestParam(required = false) Map<String, MultipartFile> file, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	appChkLst01Service.saveInspectResult(request, file, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
