package com.prafta.web.baim.baim06.controller;

import java.util.List;
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
import com.prafta.web.baim.baim06.application.param.CopySiteNodeParam;
import com.prafta.web.baim.baim06.application.param.SiteNodeAdminParam;
import com.prafta.web.baim.baim06.application.param.SiteNodeInfoParam;
import com.prafta.web.baim.baim06.application.param.SiteNodeListParam;
import com.prafta.web.baim.baim06.application.param.SiteNodeParam;
import com.prafta.web.baim.baim06.dto.request.CopySiteNodeRequest;
import com.prafta.web.baim.baim06.dto.request.SiteNodeAdminRequest;
import com.prafta.web.baim.baim06.dto.request.SiteNodeInfoRequest;
import com.prafta.web.baim.baim06.dto.request.SiteNodeListRequest;
import com.prafta.web.baim.baim06.dto.request.SiteNodeRequest;
import com.prafta.web.baim.baim06.dto.response.SiteNodeListResponse;
import com.prafta.web.baim.baim06.service.Baim06Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/baim06")
@RequiredArgsConstructor
public class Baim06Controller { 	
	
	private final Baim06Service baim06Service;
	private final JwtUtil jwtUtil;
	
	@GetMapping("/site-node-lists")
    public ResponseEntity<?> getSiteNodeList(@ModelAttribute SiteNodeListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	SiteNodeListResponse response = baim06Service.selectSiteNodeList(SiteNodeListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	@PostMapping("/save-site-nodes")
	public ResponseEntity<?> saveSiteNode(@RequestBody List<SiteNodeInfoRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization) {
	
		baim06Service.saveSiteNode(SiteNodeInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@PostMapping("/delete-site-nodes")
	public ResponseEntity<?> deleteSiteNode(@RequestBody SiteNodeRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		baim06Service.deleteSiteNode(SiteNodeParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@PostMapping("/delete-site-all-nodes")
	public ResponseEntity<?> deleteSiteAllNode(@RequestBody SiteNodeRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		baim06Service.deleteSiteAllNode(SiteNodeParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@PostMapping("/copy-site-nodes")
	public ResponseEntity<?> copySiteNode(@RequestBody CopySiteNodeRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		baim06Service.copySiteNode(CopySiteNodeParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@PostMapping("/save-site-node-main-admin")
	public ResponseEntity<?> saveSiteNodeMainAdmin(@RequestBody SiteNodeAdminRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		baim06Service.saveSiteNodeMainAdmin(SiteNodeAdminParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@PostMapping("/save-site-node-sub-admin")
	public ResponseEntity<?> saveSiteNodeSubAdmin(@RequestBody SiteNodeAdminRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		baim06Service.saveSiteNodeSubAdmin(SiteNodeAdminParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
	
	@PostMapping("/delete-site-node-admin")
	public ResponseEntity<?> deleteSiteNodeAdmin(@RequestBody SiteNodeAdminRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		baim06Service.deleteSiteNodeAdmin(SiteNodeAdminParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
}
