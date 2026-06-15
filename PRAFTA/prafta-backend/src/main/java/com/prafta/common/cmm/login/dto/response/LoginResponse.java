package com.prafta.common.cmm.login.dto.response;

import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtScope;

public record LoginResponse(
	String cmpnyCd
	, String userCd
	, String userId
	, String userNm
	, String authCd
	, String authLevel
	, String siteCd
	, String siteNo
	, String siteNm
	, String nodeCd
	, String nodeNm
	, String refreshToken
	, String token
	// PRAFTA-036: 인증대기 분기. 'PHONE_AUTH' 면 클라이언트는 휴대폰 인증 흐름으로 라우팅.
	// PRAFTA-037-F1: 'PASSWORD_CHANGE' 면 클라이언트는 강제 비밀번호 변경 흐름으로 라우팅.
	// 정상 로그인은 null/빈 값.
	, String accountStatus
	, String nextStep
	// PRAFTA-037-F1: 첫 로그인(PWD_CHG_DTIME IS NULL) 사용자 강제 PW 변경 플래그.
	// nextStep='PASSWORD_CHANGE' 와 함께 노출되며, 클라이언트가 둘 중 하나만 봐도 분기 가능.
	, Boolean mustChangePassword
	// prafta-app-025 J1-4: 고용형태[SYS041] REGULAR/CONTRACT/DAILY/EXECUTIVE.
	// 클라이언트는 'DAILY' 일 때 근태조회 카드/탭/요청 진입점을 숨긴다(세션 저장 후 라운드트립 없이 판정).
	, String employmentType
) {
	// 정책 §11.1 / §11.5: 로그인 응답 페이로드에서 자격 증명(비밀번호 해시) 및 PII(휴대폰/이메일) 제거.
	// 필요한 화면은 인증된 별도 API(/webApi/user01/user-info-lists)로 조회한다.
	public static LoginResponse from(UserResult userResult, String refreshToken, String token) {

		if (userResult == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (refreshToken == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (token == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        // PRAFTA-037-F1: PWD_CHG_DTIME IS NULL → 첫 PW 변경 미완료 → 강제 변경 흐름 합류.
        boolean mustChange = userResult.pwdChgDtime() == null
                || userResult.pwdChgDtime().isBlank();

        return new LoginResponse(
    		userResult.cmpnyCd()
    		, userResult.userCd()
            , userResult.userId()
            , userResult.userNm()
            , userResult.authCd()
            , userResult.authLevel()
            , userResult.siteCd()
            , userResult.siteNo()
            , userResult.siteNm()
            , userResult.nodeCd()
            , userResult.nodeNm()
            , refreshToken
            , token
            , userResult.accountStatus()
            , mustChange ? "PASSWORD_CHANGE" : null
            , mustChange
            , userResult.employmentType() // prafta-app-025 J1-4: 일용직 화면 숨김 신호.
        );
    }

	/**
	 * PRAFTA-036 — 인증대기(ACCOUNT_STATUS='04') 계정 로그인 시 정식 토큰 대신
	 * 임시 scope 토큰을 발급하여 응답한다. 일반 endpoint 는 접근 불가, 휴대폰 인증 전용.
	 */
	public static LoginResponse phoneAuthPending(UserResult userResult, String phoneAuthToken) {
		if (userResult == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (phoneAuthToken == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new LoginResponse(
			userResult.cmpnyCd()
			, userResult.userCd()
			, userResult.userId()
			, userResult.userNm()
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, phoneAuthToken
			, "04"
			, JwtScope.PHONE_AUTH
			, null
			, null // prafta-app-025 J1-4: 인증대기 게이트 — MainView 미진입이라 미노출.
		);
	}

	/**
	 * PRAFTA-COM-008-E-8 — 기본 근무타입 미설정(교대 비소속) 계정 로그인 시,
	 * 정식 토큰 대신 임시 scope=DEFAULT_SCH 토큰을 발급하여 응답한다.
	 * 클라이언트는 nextStep='DEFAULT_SCH' 분기로 게이트 화면을 띄우고 /login/set-default-sch 만 호출 가능.
	 */
	public static LoginResponse defaultSchPending(UserResult userResult, String defaultSchToken) {
		if (userResult == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (defaultSchToken == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new LoginResponse(
			userResult.cmpnyCd()
			, userResult.userCd()
			, userResult.userId()
			, userResult.userNm()
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, defaultSchToken
			, userResult.accountStatus()
			, JwtScope.DEFAULT_SCH
			, null
			, null // prafta-app-025 J1-4: 기본 근무타입 게이트 — MainView 미진입이라 미노출.
		);
	}
}
