package com.prafta.common.cmm.baseinfo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.cmm.baseinfo.application.param.AppMenuListParam;
import com.prafta.common.cmm.baseinfo.application.param.BaseInfoListParam;
import com.prafta.common.cmm.baseinfo.application.param.BaseInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.CmpnyInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.MenuListParam;
import com.prafta.common.cmm.baseinfo.application.param.SiteInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.SiteNodeListParam;
import com.prafta.common.cmm.baseinfo.application.param.SystInfoListParam;
import com.prafta.common.cmm.baseinfo.application.param.SystInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.TermsDetailInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.UserIdDupleCheckParam;
import com.prafta.common.cmm.baseinfo.application.param.UserIdInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.UserInfoListParam;
import com.prafta.common.cmm.baseinfo.application.param.UserPasswordParam;
import com.prafta.common.cmm.baseinfo.application.param.UserSmsAuthNoCheckParam;
import com.prafta.common.cmm.baseinfo.application.param.UserSmsAuthNoParam;
import com.prafta.common.cmm.baseinfo.application.param.WebMenuListParam;
import com.prafta.common.cmm.baseinfo.dto.request.AppMenuListRequest;
import com.prafta.common.cmm.baseinfo.dto.request.BaseInfoListRequest;
import com.prafta.common.cmm.baseinfo.dto.request.BaseInfoRequest;
import com.prafta.common.cmm.baseinfo.dto.request.CmpnyInfoRequest;
import com.prafta.common.cmm.baseinfo.dto.request.MenuListRequest;
import com.prafta.common.cmm.baseinfo.dto.request.SiteInfoRequest;
import com.prafta.common.cmm.baseinfo.dto.request.SiteNodeListRequest;
import com.prafta.common.cmm.baseinfo.dto.request.SystInfoListRequest;
import com.prafta.common.cmm.baseinfo.dto.request.SystInfoRequest;
import com.prafta.common.cmm.baseinfo.dto.request.TermsDetailInfoRequest;
import com.prafta.common.cmm.baseinfo.dto.request.UserIdDupleCheckRequest;
import com.prafta.common.cmm.baseinfo.dto.request.UserIdInfoRequest;
import com.prafta.common.cmm.baseinfo.dto.request.UserInfoListRequest;
import com.prafta.common.cmm.baseinfo.dto.request.UserPasswordRequest;
import com.prafta.common.cmm.baseinfo.dto.request.UserSmsAuthNoCheckRequest;
import com.prafta.common.cmm.baseinfo.dto.request.UserSmsAuthNoRequest;
import com.prafta.common.cmm.baseinfo.dto.request.WebMenuListRequest;
import com.prafta.common.cmm.baseinfo.dto.response.AppMenuListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.BaseInfoListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.BaseInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.CmpnyInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.MenuListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.SiteInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.SiteNodeListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.SystInfoListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.SystInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.TermsDetailInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.UserIdDupleCheckResponse;
import com.prafta.common.cmm.baseinfo.dto.response.UserIdInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.UserInfoListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.WebMenuListResponse;
import com.prafta.common.cmm.baseinfo.service.BaseinfoService;
import com.prafta.common.annotation.NoAuth;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@NoAuth
@RestController
@RequestMapping("/baseinfo")
@RequiredArgsConstructor		// 롬복이 final 필드로 생성자 자동 생성
public class BaseinfoController { 	
	
	private final BaseinfoService baseinfoService;
	private final JwtUtil jwtUtil;

	/* 공통코드 조회 */
	@NoAuth
	@GetMapping("/syst-info-lists")
    public ResponseEntity<?> getSystinfoList(@ModelAttribute SystInfoListRequest request) {
		
		SystInfoListResponse response = baseinfoService.selectSystinfoList(SystInfoListParam.from(request));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
	/* 공통코드 단일 조회 */
	@NoAuth
	@GetMapping("/syst-infos")
    public ResponseEntity<?> getSystinfo(@ModelAttribute SystInfoRequest request) {
		
		SystInfoResponse response = baseinfoService.selectSystinfo(SystInfoParam.from(request));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
	/* 운영사별 기초코드 조회 */
    @GetMapping("/base-info-lists")
    public ResponseEntity<?> getBaseinfoList(@ModelAttribute BaseInfoListRequest request, @RequestHeader(value = "Authorization", required = true) String authorization) {
    	
    	BaseInfoListResponse response = baseinfoService.selectBaseinfoList(BaseInfoListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /* 운영사별 기초코드 단일 조회 */
	@GetMapping("/base-infos")
    public ResponseEntity<?> getBaseinfo(@ModelAttribute BaseInfoRequest request, @RequestHeader(value = "Authorization", required = true) String authorization) {
		
		BaseInfoResponse response = baseinfoService.selectBaseinfo(BaseInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
	/* 회사명 조회  */
	@NoAuth
    @GetMapping("/cmpny-infos")
    public ResponseEntity<?> getCmpnyInfo(@ModelAttribute CmpnyInfoRequest request) {
    	CmpnyInfoResponse response = baseinfoService.selectCmpnyInfo(CmpnyInfoParam.from(request));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /* WEB 메뉴 조회 */
    @GetMapping("/web-menu-lists")
    public ResponseEntity<?> getWebMenuList(@ModelAttribute WebMenuListRequest request, @RequestHeader(value = "Authorization", required = true) String authorization) {
    	WebMenuListResponse response = baseinfoService.selectWebMenuList(WebMenuListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /* APP 메뉴 조회 */
    @GetMapping("/app-menu-lists")
    public ResponseEntity<?> getAppMenuList(@ModelAttribute AppMenuListRequest request, @RequestHeader(value = "Authorization", required = true) String authorization) {
    	AppMenuListResponse retList = baseinfoService.selectAppMenuList(AppMenuListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
    
    /* 사용자 ID 중복체크 */
    @NoAuth
    @GetMapping("/user-id-duple-checks")
    public ResponseEntity<?> getUserIdDupleCheck(@ModelAttribute UserIdDupleCheckRequest request) {

    	UserIdDupleCheckResponse response = baseinfoService.getUserIdDupleCheck(UserIdDupleCheckParam.from(request));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /* SMS 발송 */
    @NoAuth
    @PostMapping("/sms-auth-sends")
    public ResponseEntity<?> insertSmsAuthNo(@RequestBody UserSmsAuthNoRequest request) {
        baseinfoService.insertSmsAuthNo(UserSmsAuthNoParam.from(request));
        
        return ResponseEntity.status(HttpStatus.OK).build();
    }
    
    /* SMS 인증번호 확인  */
    @NoAuth
    @PostMapping("/sms-auth-checks")
    public ResponseEntity<?> userSmsAuthCheck(@RequestBody UserSmsAuthNoCheckRequest request) {
    	baseinfoService.userSmsAuthCheck(UserSmsAuthNoCheckParam.from(request));
    	
        return ResponseEntity.status(HttpStatus.OK).build();
    }
    
    /* 사업장 리스트 조회 */
    @GetMapping("/site-lists")
    public ResponseEntity<?> getSiteInfoList(@ModelAttribute SiteInfoRequest request, @RequestHeader(value = "Authorization", required = true) String authorization) {
    	
    	SiteInfoResponse response = baseinfoService.selectSiteInfoList(SiteInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /* 사업장 소속 부서 리스트 조회 */
    @GetMapping("/site-node-lists")
    public ResponseEntity<?> getSiteNodeList(@ModelAttribute SiteNodeListRequest request, @RequestHeader(value = "Authorization", required = true) String authorization) {
    	
    	SiteNodeListResponse response = baseinfoService.selectSiteNodeList(SiteNodeListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
        
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /* 사용자 메뉴 조회  */
    @GetMapping("/menu-list")
    public ResponseEntity<?> getMenuList(@ModelAttribute MenuListRequest request, @RequestHeader(value = "Authorization", required = true) String authorization) {
    	
    	MenuListResponse response = baseinfoService.selectMenuList(MenuListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /* 사용자 정보 조회 */
    @GetMapping("/user-info-lists")
    public ResponseEntity<?> getUserInfoList(@ModelAttribute UserInfoListRequest request, @RequestHeader(value = "Authorization", required = true) String authorization) {
    	
    	UserInfoListResponse response = baseinfoService.selectUserInfoList(UserInfoListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @NoAuth
    @GetMapping("/user-ids")
    public ResponseEntity<?> getUserIdInfo(@ModelAttribute UserIdInfoRequest request) {
    	UserIdInfoResponse response = baseinfoService.selectUserIdInfo(UserIdInfoParam.from(request));
        
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @NoAuth
    @PostMapping("/update-user-password")
    public ResponseEntity<?> updateUserPw(@RequestBody UserPasswordRequest request) {
    	
    	baseinfoService.updateUserPw(UserPasswordParam.from(request));
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
    
    @NoAuth
    @GetMapping("/terms-detail-infos")
    public ResponseEntity<?> getTermsDetailInfo(@ModelAttribute TermsDetailInfoRequest request) {
    	TermsDetailInfoResponse response = baseinfoService.selectTermsDetailInfo(TermsDetailInfoParam.from(request));

    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
