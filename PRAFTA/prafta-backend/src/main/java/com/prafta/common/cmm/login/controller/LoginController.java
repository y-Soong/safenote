package com.prafta.common.cmm.login.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.cmm.login.application.param.AuthMenuInfoParam;
import com.prafta.common.cmm.login.application.param.LoginParam;
import com.prafta.common.cmm.login.application.param.LogoutParam;
import com.prafta.common.cmm.login.application.param.UserJoinParam;
import com.prafta.common.cmm.login.application.param.UserTermsAgreementCheckParam;
import com.prafta.common.cmm.login.application.param.VerifyPhoneAuthParam;
import com.prafta.common.cmm.login.dto.request.AuthMenuInfoRequest;
import com.prafta.common.cmm.login.dto.request.LoginRequest;
import com.prafta.common.cmm.login.dto.request.SetDefaultSchRequest;
import com.prafta.common.cmm.login.dto.request.UserJoinRequest;
import com.prafta.common.cmm.login.dto.request.VerifyPhoneAuthRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.login.LoginErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.cmm.login.dto.response.AuthLogoutResponse;
import com.prafta.common.cmm.login.dto.response.DefaultSchOptionsResponse;
import com.prafta.common.cmm.login.dto.response.LoginResponse;
import com.prafta.common.cmm.login.dto.response.UserJoinResponse;
import com.prafta.common.cmm.login.dto.response.UserTermsAgreementCheckResponse;
import com.prafta.common.cmm.login.mapper.LoginMapper;
import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.cmm.sch.mapper.DefaultSchGenMapper;
import com.prafta.common.cmm.sch.service.DefaultSchOptionService;
import com.prafta.common.cmm.login.service.LoginService;
import com.prafta.common.security.JwtScope;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.ClientIpExtractor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoAuth
@Validated
@RestController
@RequestMapping("/login")
@RequiredArgsConstructor // 롬복이 final 필드로 생성자 자동 생성
public class LoginController { 	
	
	private final JwtUtil jwtUtil;
	private final LoginService loginService;
	// PRAFTA-COM-008-E-8 — 게이트 옵션 목록 조회(토큰 식별 사용자 → 사업장 활성 근무타입).
	private final DefaultSchOptionService defaultSchOptionService;
	private final DefaultSchGenMapper defaultSchGenMapper;
	private final LoginMapper loginMapper;
	
    // 보안 수정(PRAFTA-006-001): 자격증명이 URL 쿼리스트링/서버 로그/Referer에 노출되지 않도록 POST + JSON 본문으로 전환
    @PostMapping("/login")
    public ResponseEntity<?> Login(@Valid @RequestBody LoginRequest loginRequest, @RequestHeader(value = "X-Client-Type", required = false, defaultValue = "WEB") String clientType, HttpServletRequest httpRequest) {
    	// prafta-com-003 C3: 로그인 IP 는 서버가 추출(디바이스 로그인 이력 LOGIN_IP). 클라 신뢰 안 함.
    	String ipAddr = ClientIpExtractor.extract(httpRequest);
    	LoginResponse loginResponse = loginService.Login(LoginParam.from(loginRequest, clientType, ipAddr));

    	return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }

    /**
     * 로그아웃: 활성 세션 revoke (B안: 일반 API는 JWT만 검증, refresh에서만 DB검증)
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthLogoutResponse> logout(
		@RequestHeader(value = "X-Client-Type", required = false, defaultValue = "WEB") String clientType
		, @RequestHeader(value = "Authorization", required = false) String authorization
		// prafta-com-015 015-3: 앱은 본문에 gv_deviceId 를 동봉(현재 기기 푸시 토큰 정리용). 웹은 미전송 → null → skip.
		, @RequestBody(required = false) java.util.Map<String, Object> body
    ) {
    	String bodyDeviceId = (body == null || body.get("gv_deviceId") == null)
    			? null : String.valueOf(body.get("gv_deviceId"));
    	loginService.logout(LogoutParam.from(clientType, jwtUtil.getAllClaimsAsMap(authorization), bodyDeviceId));

    	return ResponseEntity.status(HttpStatus.OK).build();
    }
    
    /**
     * 셀프가입(회원가입) 접수.
     *
     * <p>소정-04: 가입은 즉시 활성화가 아니라 <b>관리자 승인 대기('06')</b>로 끝난다.
     * 응답 body 의 {@code accountStatus='06'} / {@code nextStep='JOIN_APPROVAL_PENDING'} 로
     * 클라이언트가 승인대기 안내 화면으로 분기한다(기존 빈 body → 상태 신호 추가).
     */
    @PostMapping("/insert-user-info")
    public ResponseEntity<?> insertUserInfo(@Valid @RequestBody UserJoinRequest request, HttpServletRequest httpRequest) {

    	// [security M-1] 비로그인 경로라 행위자 식별이 불가능하므로 IP/UA 를 감사 컨텍스트로 넘긴다
    	//   (재가입 = 거부 계정 부활 적재에 사용). IP 는 서버가 추출한다(클라 신뢰 금지).
    	AuditContext auditContext = new AuditContext(
    			ClientIpExtractor.extract(httpRequest),
    			httpRequest != null ? httpRequest.getHeader("User-Agent") : null);

    	UserJoinResponse response = loginService.insertUserInfo(UserJoinParam.from(request), auditContext);

    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @GetMapping("/user-terms-agreement-check")
    public ResponseEntity<?> userTermsAgrementCheck(@RequestHeader("Authorization") String authorization) {
    	UserTermsAgreementCheckResponse response = loginService.userTermsAgrementCheck(UserTermsAgreementCheckParam.from(jwtUtil.getAllClaimsAsMap(authorization)));
   	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @PostMapping("/update-auth-menu-info")
    public ResponseEntity<?> updateAuthMenuInfo(@RequestBody List<AuthMenuInfoRequest> request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	loginService.updateAuthMenuInfo(AuthMenuInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * PRAFTA-036 — 인증대기('04') 계정의 휴대폰 본인인증 후 활성화.
     *
     * <p>{@code Authorization: Bearer <임시 scope=PHONE_AUTH 토큰>} 필수.
     * 인증 성공 시 정식 토큰/리프레시 + LoginResponse 응답(일반 로그인과 동일 형식).
     */
    @PostMapping("/verify-phone-auth")
    public ResponseEntity<?> verifyPhoneAuth(
            @Valid @RequestBody VerifyPhoneAuthRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization,
            @RequestHeader(value = "X-Client-Type", required = false, defaultValue = "WEB") String clientType) {

        // 임시 토큰 검증 (만료/서명) — 일반 토큰 검증 흐름 재사용.
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiException(LoginErrorCode.LOGIN_400_012);
        }
        String pureToken = authorization.substring(7);
        if (!jwtUtil.validateToken(pureToken)) {
            throw new ApiException(LoginErrorCode.LOGIN_400_012);
        }

        // 임시 토큰의 scope claim 추출.
        String scope = jwtUtil.parseToken(pureToken).get("gv_scope", String.class);
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        LoginResponse response = loginService.verifyPhoneAuth(
                VerifyPhoneAuthParam.from(request, tokenInfo, scope, clientType));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 휴대폰 본인인증 팝업 자동기입 — PHONE_AUTH 임시 토큰 식별 사용자의 등록 휴대폰 반환.
     * 토큰 claim(cmpnyCd/userCd)으로만 대상 식별(IDOR 방지). 클라가 임의 번호로 바꾸지 못하게 잠금 표시용.
     */
    @GetMapping("/phone-auth-phone")
    public ResponseEntity<?> getPhoneAuthPhone(
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        TokenInfo tokenInfo = requirePhoneAuthScope(authorization);
        String mblNo = loginService.getPhoneAuthTargetPhone(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());

        return ResponseEntity.status(HttpStatus.OK).body(java.util.Map.of("mblNo", mblNo == null ? "" : mblNo));
    }

    /**
     * PRAFTA-COM-008-E-8 — 게이트 화면용 사업장 활성 근무타입 목록.
     *
     * <p>{@code Authorization: Bearer <scope=DEFAULT_SCH 임시 토큰>} 필수.
     * 토큰 claim 의 cmpnyCd/userCd 로만 대상 식별(IDOR 방지) → 사용자 SITE_CD → USE_YN='Y' 옵션.
     */
    @GetMapping("/default-sch-options")
    public ResponseEntity<?> getDefaultSchOptions(
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        TokenInfo tokenInfo = requireDefaultSchScope(authorization);

        String siteCd = resolveUserSiteCd(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());
        var schedules = (siteCd == null)
                ? java.util.List.<com.prafta.common.cmm.sch.vo.SchOptionVO>of()
                : defaultSchOptionService.getActiveSchOptions(tokenInfo.gv_cmpnyCd(), siteCd);

        return ResponseEntity.status(HttpStatus.OK).body(new DefaultSchOptionsResponse(schedules));
    }

    /**
     * PRAFTA-COM-008-E-8 — 기본 근무타입 게이트 통과(설정 저장 + 즉시 생성 + 정식 LoginResponse).
     *
     * <p>{@code Authorization: Bearer <scope=DEFAULT_SCH 임시 토큰>} 필수. 일반 로그인과 동일 형식 응답.
     */
    @PostMapping("/set-default-sch")
    public ResponseEntity<?> setDefaultSch(
            @RequestBody SetDefaultSchRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization,
            @RequestHeader(value = "X-Client-Type", required = false, defaultValue = "WEB") String clientType) {

        TokenInfo tokenInfo = requireDefaultSchScope(authorization);

        LoginResponse response = loginService.setDefaultSch(
                tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd(), request.getDefaultSchCd(), clientType);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * scope=DEFAULT_SCH 임시 토큰 검증(만료/서명/scope). 통과 시 claim 을 TokenInfo 로 반환.
     * verify-phone-auth 검증 패턴 미러.
     */
    private TokenInfo requireDefaultSchScope(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiException(LoginErrorCode.LOGIN_400_012);
        }
        String pureToken = authorization.substring(7);
        if (!jwtUtil.validateToken(pureToken)) {
            throw new ApiException(LoginErrorCode.LOGIN_400_012);
        }
        String scope = jwtUtil.parseToken(pureToken).get("gv_scope", String.class);
        if (!JwtScope.DEFAULT_SCH.equals(scope)) {
            throw new ApiException(LoginErrorCode.LOGIN_400_012);
        }
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isBlank()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank()) {
            throw new ApiException(LoginErrorCode.LOGIN_400_012);
        }
        return tokenInfo;
    }

    /**
     * scope=PHONE_AUTH 임시 토큰 검증(만료/서명/scope). 통과 시 claim 을 TokenInfo 로 반환.
     * requireDefaultSchScope 미러.
     */
    private TokenInfo requirePhoneAuthScope(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiException(LoginErrorCode.LOGIN_400_012);
        }
        String pureToken = authorization.substring(7);
        if (!jwtUtil.validateToken(pureToken)) {
            throw new ApiException(LoginErrorCode.LOGIN_400_012);
        }
        String scope = jwtUtil.parseToken(pureToken).get("gv_scope", String.class);
        if (!JwtScope.PHONE_AUTH.equals(scope)) {
            throw new ApiException(LoginErrorCode.LOGIN_400_012);
        }
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isBlank()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank()) {
            throw new ApiException(LoginErrorCode.LOGIN_400_012);
        }
        return tokenInfo;
    }

    /** 토큰 식별 사용자의 SITE_CD 도출(게이트 옵션 스코프). 없으면 null. */
    private String resolveUserSiteCd(String cmpnyCd, String userCd) {
        UserResult user = loginMapper.selectUserByUserCd(cmpnyCd, userCd);
        String siteCd = (user == null) ? null : user.siteCd();
        if (siteCd == null || siteCd.isBlank()) {
            siteCd = defaultSchGenMapper.selectUserSiteCd(cmpnyCd, userCd);
        }
        return (siteCd == null || siteCd.isBlank()) ? null : siteCd;
    }
}
