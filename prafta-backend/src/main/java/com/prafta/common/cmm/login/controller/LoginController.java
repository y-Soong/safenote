package com.prafta.common.cmm.login.controller;

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
import com.prafta.common.cmm.login.dto.AuthLogoutReq;
import com.prafta.common.cmm.login.dto.AuthLogoutRes;
import com.prafta.common.cmm.login.dto.LoginReqDto;
import com.prafta.common.cmm.login.dto.UserJoinReqDto;
import com.prafta.common.cmm.login.service.LoginService;
import com.prafta.common.exception.CmmApiException;
import com.prafta.common.exception.LoginFailException;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@RestController
@RequestMapping("/login")
@RequiredArgsConstructor // 롬복이 final 필드로 생성자 자동 생성
public class LoginController { 	
	
	private final JwtUtil jwtUtil;
	private final LoginService loginService;
	
    @PostMapping("/loginChk")
    public ResponseEntity<?> Login(@RequestBody  LoginReqDto dto, @RequestHeader(value = "X-Client-Type", required = false, defaultValue = "WEB") String clientType) {
    	    	
		Map<String, Object> retMap = loginService.getLoginUser(dto, clientType);
    	
    	if(retMap != null) {
    		
    		String token = jwtUtil.generateToken(
    				retMap.get("CMPNY_CD").toString()
    				, retMap.get("USER_ID").toString()
    				, retMap.get("USER_NM").toString()
    				, retMap.get("SITE_CD").toString()
    				, retMap.get("SITE_NO").toString()
    				, retMap.get("SITE_NM").toString()
    				, retMap.get("AUTH_CD").toString()
    				, retMap.get("MBL_NO").toString()
    				, retMap.get("EMAIL").toString()
			);		// 토큰 생성
    		
    		retMap.put("retMsg", "로그인에 성공했습니다.");
    		retMap.put("cmpnyCd", retMap.get("CMPNY_CD"));
    		retMap.put("userId", retMap.get("USER_ID"));
    		retMap.put("userNm", retMap.get("USER_NM"));
    		
    		retMap.put("siteCd", retMap.get("SITE_CD"));
    		retMap.put("siteNo", retMap.get("SITE_NO"));
    		retMap.put("siteNm", retMap.get("SITE_NM"));
    		
    		retMap.put("authCd", retMap.get("AUTH_CD"));
    		retMap.put("mblNo", retMap.get("MBL_NO"));
    		retMap.put("email", retMap.get("EMAIL"));
    		retMap.put("refreshToken", retMap.get("refreshToken"));
    		retMap.put("token", token);
    		
    	} else {
    		throw new LoginFailException("아이디 혹은 비밀번호를 확인해주세요.");
    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retMap);
    }
    
    /**
     * 로그아웃: 활성 세션 revoke (B안: 일반 API는 JWT만 검증, refresh에서만 DB검증)
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthLogoutRes> logout(@RequestHeader("Authorization") String authorization, @RequestHeader(value = "X-Client-Type", required = false, defaultValue = "WEB") String clientType) {
    	
        // JWT에서 사용자 식별
        Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        
        AuthLogoutReq req = AuthLogoutReq.builder()
                .clientType(clientType)
                .deviceId((String)tokenInfo.get("gv_deviceId"))
                .logoutAllYn("N")
                .build();

        int revoked = loginService.logout(req, tokenInfo);

        return ResponseEntity.status(HttpStatus.OK).body(
                AuthLogoutRes.builder()
                        .message("로그아웃되었습니다.")
                        .revokedCount(revoked)
                        .build()
        );
    }
    
    @PostMapping("/insertUserInfo")
    public ResponseEntity<?> insertUserInfo(@RequestBody UserJoinReqDto dto) {
    	
    	loginService.insertUserInfo(dto);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
    
    @PostMapping("/getUserTermsAgrChk")
    public ResponseEntity<?> getUserTermsAgrChk(@RequestBody LoginReqDto dto) {
		List<Map<String, Object>> retList = loginService.selectUserTermsAgrChk(dto);
    	
    	if(retList == null) {
    		throw new CmmApiException("조회결과가 없습니다.");
    	}
    	
    	return ResponseEntity.status(HttpStatus.OK).body(retList);
    }
    
    @PostMapping("/updateAuthMenuInfo")
    public ResponseEntity<?> updateAuthMenuInfo(@RequestBody List<LoginReqDto> dtoList, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	Map<String, Object> tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    	
    	loginService.updateAuthMenuInfo(dtoList, tokenInfo);
    	
    	return ResponseEntity.status(HttpStatus.OK).build();
    }
}
