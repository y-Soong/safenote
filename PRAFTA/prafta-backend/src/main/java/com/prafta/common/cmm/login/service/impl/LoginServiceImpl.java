package com.prafta.common.cmm.login.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.login.application.command.ActiveTokenCommand;
import com.prafta.common.cmm.login.application.command.AuthMenuInfoCommand;
import com.prafta.common.cmm.login.application.command.DeviceLoginCommand;
import com.prafta.common.cmm.login.application.command.RequiredTermsInfoCommand;
import com.prafta.common.cmm.login.application.command.UserJoinCommand;
import com.prafta.common.cmm.login.application.command.UserLogoutCommand;
import com.prafta.common.cmm.login.application.command.UserPwdFailCommand;
import com.prafta.common.cmm.login.application.command.UserPwdUnlockCommand;
import com.prafta.common.cmm.baseinfo.application.param.UserSmsAuthNoCheckParam;
import com.prafta.common.cmm.baseinfo.dto.request.UserSmsAuthNoCheckRequest;
import com.prafta.common.cmm.baseinfo.service.BaseinfoService;
import com.prafta.common.cmm.login.application.param.AuthMenuInfoParam;
import com.prafta.common.cmm.login.application.param.LoginParam;
import com.prafta.common.cmm.login.application.param.LogoutParam;
import com.prafta.common.cmm.login.application.param.UserJoinParam;
import com.prafta.common.cmm.login.application.param.UserTermsAgreementCheckParam;
import com.prafta.common.cmm.login.application.param.VerifyPhoneAuthParam;
import com.prafta.common.cmm.login.application.query.LoginQuery;
import com.prafta.common.cmm.login.application.query.UserRowLockQuery;
import com.prafta.common.cmm.login.application.query.UserTermsAgreementCheckQuery;
import com.prafta.common.cmm.login.dto.response.LoginResponse;
import com.prafta.common.cmm.login.dto.response.UserTermsAgreementCheckResponse;
import com.prafta.common.cmm.login.mapper.LoginMapper;
import com.prafta.common.cmm.login.result.RequiredTermsResult;
import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.cmm.login.result.UserTermsAgreementCheckResult;
import com.prafta.common.cmm.login.service.LoginService;
import com.prafta.common.error.login.LoginErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.common.util.PasswordHasher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService{
	private final LoginMapper loginMapper;
	private final HmacSigner hmacSigner;
	private final AesGcmCrypto aesGcmCrypto;
	private final PasswordHasher passwordHasher;
	private final JwtUtil jwtUtil;
	private final BaseinfoService baseinfoService; // PRAFTA-036 — SMS 인증번호 검증 재사용

	// PRAFTA-036 — 인증대기 분기에서 발급하는 임시 토큰 만료(분).
	private static final int PHONE_AUTH_TOKEN_TTL_MINUTES = 10;
	
	@Value("${login.lock.duration-minutes}")
	private int lockDurationMinutes;

	@Value("${login.lock.max-fail-count}")
	private int maxFailCount;
	
	public LoginResponse Login(LoginParam param) {
		
		UserResult userResult = loginMapper.Login(LoginQuery.from(param));
		
		if (userResult == null) {
			throw new ApiException(LoginErrorCode.LOGIN_400_001);
	    }
		
		// 비밀번호 인증 실패 잠금 만료일시 체크
		if(userResult.pwdLockYn().equals("Y")) {
			String unlockDtimeStr = userResult.pwdLockExpireDtime();
			
			if (unlockDtimeStr != null) {
		        LocalDateTime unlockDtime = LocalDateTime.parse(unlockDtimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		        
		        if (LocalDateTime.now().isBefore(unlockDtime)) {
		            long remainMinutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), unlockDtime);
		            throw ApiException.appendf(LoginErrorCode.LOGIN_400_003, "\n잠금 해제까지 %d분 남았습니다.", remainMinutes);
		        }
		    }
		}
		
		// 잠금 해제 시각이 지났남, 계정 상태 업데이트
	    loginMapper.userPwdUnLock(UserPwdUnlockCommand.from(userResult));
		
		String hashedPw = (String) userResult.userPw();
		
		if(!passwordHasher.matches(param.userPw(), hashedPw)) {

			loginMapper.updateUserPwdFail(UserPwdFailCommand.from(userResult, lockDurationMinutes, maxFailCount));

			throw new ApiException(LoginErrorCode.LOGIN_400_001);
		}

		// PRAFTA-036: ACCOUNT_STATUS 분기 — 탈퇴('03') 차단, 인증대기('04')는 정식 토큰 대신 임시 scope 토큰만 발급.
		String accountStatus = userResult.accountStatus();
		if ("03".equals(accountStatus)) {
			throw new ApiException(LoginErrorCode.LOGIN_400_014);
		}
		if ("04".equals(accountStatus)) {
			String phoneAuthToken = jwtUtil.generatePhoneAuthScopeToken(
					userResult.cmpnyCd(), userResult.userCd(), PHONE_AUTH_TOKEN_TTL_MINUTES);
			log.info("인증대기 계정 로그인 — 임시 토큰 발급(scope=PHONE_AUTH). userCd={}", userResult.userCd());
			return LoginResponse.phoneAuthPending(userResult, phoneAuthToken);
		}

		// 사용자 정보 LOCK 잡기
		loginMapper.lockUserRow(UserRowLockQuery.from(userResult));
		
		String tokenId = UUID.randomUUID().toString().replace("-", "");
		
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[64]; // 64바이트면 충분히 강함
		random.nextBytes(bytes);
		String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		String refreshTokenHash = hmacSigner.hmacSha256Base64Url(refreshToken);
		
		// 정책 §3.4: Refresh Token 유효기간 48시간(2일)
		ActiveTokenCommand activeTokenCommand = ActiveTokenCommand.from(userResult, tokenId, param.clientType(), refreshTokenHash, "2");
		
		// 기존 세션 revoke
		loginMapper.revokeActiveToken(activeTokenCommand);
		// 신규 세션 insert
		loginMapper.insertAuthToken(activeTokenCommand);
		
		// 정책 §11.1에 따라 로그인 응답 페이로드에서 휴대폰/이메일 평문을 제거.
		// 필요한 화면은 인증된 별도 API(/webApi/user01/user-info-lists)로 조회한다.
		String token = jwtUtil.generateToken(userResult);

		// 사용자 로그인 시간 기록
		loginMapper.updateUserLastLoginDtime(userResult.userCd());

		// prafta-com-003 C3: 디바이스 식별 기반 부정탐지 baseline 적재(디바이스 upsert + 로그인 이력 INSERT).
		//   전체 try-catch 로 격리 — 적재 실패가 로그인 자체를 막거나 롤백하지 않게 한다(com-001 체크인 훅 패턴).
		//   deviceId 가 없으면(웹 로그인/구버전 앱) skip. deviceId 는 클라 제공값(위조 가능)이라
		//   식별/인가에는 쓰지 않고 이력/디바이스 상태 적재에만 사용한다.
		recordDeviceLogin(userResult, param);

		return LoginResponse.from(userResult, refreshToken, token);
	}

	/**
	 * prafta-com-003 C3 — 로그인 성공 직후 디바이스/로그인 이력 적재(예외 격리).
	 *
	 * <p>main 로그인 경로에만 적용한다(메인세션 확정 A: verifyPhoneAuth 사전검증 경로는 미적용).
	 * deviceId 가 비어 있으면(웹/구버전 앱) 아무 것도 하지 않는다. 적재 중 어떤 예외가 나도
	 * 로그인 흐름에는 영향을 주지 않으며 log.error 로만 남긴다.
	 */
	private void recordDeviceLogin(UserResult userResult, LoginParam param) {
		try {
			if (param == null || param.deviceId() == null || param.deviceId().isBlank()) {
				return; // 디바이스ID 미전송(웹/구버전 앱) → 적재 대상 아님.
			}
			DeviceLoginCommand command = new DeviceLoginCommand(
					userResult.cmpnyCd()
					, param.deviceId()
					, userResult.userCd()
					, param.deviceType()
					, param.deviceModel()
					, param.osVersion()
					, param.appVersion()
					, param.clientType()
					, param.ipAddr()
					, userResult.userCd());
			loginMapper.upsertUserDevice(command);
			loginMapper.insertDeviceLoginHist(command);
			// PII(기기ID/IP) 평문 로그 금지 — 식별 키만 남긴다.
			log.info("디바이스 로그인 이력 적재 완료 — userCd={}, clientType={}", userResult.userCd(), param.clientType());
		} catch (Exception e) {
			log.error("디바이스 로그인 이력 적재 실패(로그인 영향 없음) — userCd={}", userResult.userCd(), e);
		}
	}
	
	@Override
    @Transactional
    public void logout(LogoutParam param) {

        loginMapper.revokeAllActiveTokens(UserLogoutCommand.from(param));
        loginMapper.revokeActiveTokenByClient(UserLogoutCommand.from(param));
    }
	
	@Transactional
    public void insertUserInfo(UserJoinParam param) {

		// 0) PRAFTA-046: 가입 대상 노드에 정/부 관리자가 존재해야 가입 가능 (fail-closed).
		//    노드 미존재/관리자 미지정이면 가입 거부 — 회사 내부구조 노출 없는 친화 메시지.
		int nodeHasAdmin = loginMapper.selectNodeHasAdmin(param.cmpnyCd(), param.siteCd(), param.nodeCd());
		if (nodeHasAdmin == 0) {
			throw new ApiException(LoginErrorCode.LOGIN_400_015);
		}

		String userPw = "";

        // 1) 비밀번호 해시 (평문 저장 금지)
        if (param.userPw() != null) {
        	userPw = passwordHasher.hash(param.userPw());
        }

        // 2) 정규화
        String phoneNorm = Normalizers.normalizePhone(param.mblNo());
        String emailNorm = Normalizers.normalizeEmail(param.email());
        String birthNorm = Normalizers.normalizeBirth(param.birthDt());

        // 3) ENC (AES-GCM)
        String phoneEnc = (phoneNorm == null) ? null : aesGcmCrypto.encrypt(phoneNorm);
        String emailEnc = (emailNorm == null) ? null : aesGcmCrypto.encrypt(emailNorm);
        String birthEnc = (birthNorm == null) ? null : aesGcmCrypto.encrypt(birthNorm);

        // 4) HMAC 인덱스 (equals/중복/계정찾기)
        // 회사 단위 유니크면 cmpnyCd 섞는 걸 추천
        String phoneHmac = (phoneNorm == null) ? null : hmacSigner.hmacSha256Base64Url(phoneNorm);
        String emailHmac = (emailNorm == null) ? null : hmacSigner.hmacSha256Base64Url(emailNorm);

        // 5) 파생값
        String phoneLast4 = Normalizers.last4(phoneNorm);
        String emailDomain = Normalizers.emailDomain(emailNorm);
        
        String userCd = loginMapper.selectUserCd(param.cmpnyCd());

        UserJoinCommand userJoinCommand = UserJoinCommand.from(param, userCd, userPw, phoneEnc, phoneHmac, phoneLast4, emailEnc, emailHmac, emailDomain, birthEnc);
        
        // 6) INSERT
        loginMapper.insertUserInfo(userJoinCommand);
        loginMapper.insertUserSiteAuth(userJoinCommand);

        // 약관 동의 로직은 그대로
        List<RequiredTermsResult> RequiredTermsResultList = loginMapper.selectRequiredTermsList();
        if (RequiredTermsResultList == null || RequiredTermsResultList.isEmpty()) {
            throw new ApiException(LoginErrorCode.LOGIN_500_001);
        }
        for (RequiredTermsResult RequiredTermsResult : RequiredTermsResultList) {
        	
            loginMapper.insertTermsUserAgrMgmt(RequiredTermsInfoCommand.from(userCd, param, RequiredTermsResult));
        }
    }
	
	public UserTermsAgreementCheckResponse userTermsAgrementCheck(UserTermsAgreementCheckParam param) {
		
		UserTermsAgreementCheckResponse response = null;
		
		List<UserTermsAgreementCheckResult> userTermsAgreementCheckResultList = loginMapper.selectUserTermsAgreementCheck(UserTermsAgreementCheckQuery.from(param));
		
		if(userTermsAgreementCheckResultList != null && userTermsAgreementCheckResultList.size() > 0) {
			response = UserTermsAgreementCheckResponse.builder()
													.userTermsAgreementCheckList(userTermsAgreementCheckResultList)
													.build();
		}
		
		return response;
	}

	public void updateAuthMenuInfo(AuthMenuInfoParam Param) {

		if(Param != null && Param.systValDCdList().size() > 0) {
			for(String systValDCd : Param.systValDCdList()) {
				loginMapper.mergeAuthMenuInfo(AuthMenuInfoCommand.from(systValDCd, Param.userId()));
			}
		}
	}

	/**
	 * PRAFTA-036 — 인증대기('04') 계정의 휴대폰 본인인증을 처리하고 계정을 '01' 로 전이한다.
	 *
	 * <p>플로우:
	 * <ol>
	 *   <li>임시 토큰의 scope 검증은 Param.from 에서 완료(여기까지 오면 scope=PHONE_AUTH).</li>
	 *   <li>대상 사용자 조회 + 상태 검증(현재 '04' 가 아니면 거부).</li>
	 *   <li>입력 휴대폰 정규화 → HMAC 계산 → 다른 사용자 보유 여부(UX_TB_USER_MBL_NO) 사전 진단.</li>
	 *   <li>{@link BaseinfoService#userSmsAuthCheck} 재사용으로 인증번호 검증.</li>
	 *   <li>관리자가 등록한 휴대폰과 다르면 MBL_NO_ENC/HMAC/LAST4 갱신.</li>
	 *   <li>ACCOUNT_STATUS 를 '01' 로 전이 + 정식 토큰/리프레시 발급.</li>
	 * </ol>
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public LoginResponse verifyPhoneAuth(VerifyPhoneAuthParam param) {

		// 1) 대상 사용자 + 상태 검증.
		UserResult userResult = loginMapper.selectUserByUserCd(param.gvCmpnyCd(), param.gvUserCd());
		if (userResult == null) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}
		if (!"04".equals(userResult.accountStatus())) {
			throw new ApiException(LoginErrorCode.LOGIN_400_013);
		}

		// 2) 입력 휴대폰 정규화.
		String phoneNorm = Normalizers.normalizePhone(param.mblNo());
		if (phoneNorm == null || phoneNorm.length() < 10 || phoneNorm.length() > 11) {
			throw new ApiException(LoginErrorCode.LOGIN_400_010);
		}
		String phoneHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);

		// 3) HMAC 충돌 사전 진단 (다른 사용자가 이미 보유 중인지).
		int conflict = loginMapper.selectUserMblHmacConflict(param.gvCmpnyCd(), param.gvUserCd(), phoneHmac);
		if (conflict > 0) {
			throw new ApiException(LoginErrorCode.LOGIN_400_011);
		}

		// 4) SMS 인증번호 검증 (BaseinfoService 재사용).
		UserSmsAuthNoCheckRequest smsReq = new UserSmsAuthNoCheckRequest();
		smsReq.setCmpnyCd(param.gvCmpnyCd());
		smsReq.setMblNo(phoneNorm);
		smsReq.setCertNo(param.certNo());
		baseinfoService.userSmsAuthCheck(UserSmsAuthNoCheckParam.from(smsReq));

		// 5) 관리자 등록 휴대폰과 인증한 휴대폰이 다르면 MBL_NO_* 갱신.
		String storedEnc = userResult.mblNoEnc();
		String storedNorm = (storedEnc == null) ? null : aesGcmCrypto.decrypt(storedEnc);
		if (storedNorm == null || !storedNorm.equals(phoneNorm)) {
			String phoneEnc = aesGcmCrypto.encrypt(phoneNorm);
			String phoneLast4 = Normalizers.last4(phoneNorm);
			loginMapper.updateUserMblNo(param.gvCmpnyCd(), param.gvUserCd(), phoneEnc, phoneHmac, phoneLast4);
		}

		// 6) ACCOUNT_STATUS = '01' 전이.
		int activated = loginMapper.activateAccount(param.gvCmpnyCd(), param.gvUserCd());
		if (activated == 0) {
			// 동시성으로 다른 트랜잭션이 이미 활성화했거나 상태가 변경됨.
			throw new ApiException(LoginErrorCode.LOGIN_400_013);
		}

		log.info("인증대기 계정 활성화 완료 — userCd={}, 상태 04→01", param.gvUserCd());

		// 7) 갱신된 사용자 정보로 정식 토큰 + 리프레시 발급 (Login 흐름 동일).
		UserResult activatedUser = loginMapper.selectUserByUserCd(param.gvCmpnyCd(), param.gvUserCd());
		if (activatedUser == null) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}

		String tokenId = java.util.UUID.randomUUID().toString().replace("-", "");
		java.security.SecureRandom random = new java.security.SecureRandom();
		byte[] bytes = new byte[64];
		random.nextBytes(bytes);
		String refreshToken = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		String refreshTokenHash = hmacSigner.hmacSha256Base64Url(refreshToken);

		ActiveTokenCommand activeTokenCommand = ActiveTokenCommand.from(
				activatedUser, tokenId, param.clientType(), refreshTokenHash, "2");
		loginMapper.revokeActiveToken(activeTokenCommand);
		loginMapper.insertAuthToken(activeTokenCommand);

		String token = jwtUtil.generateToken(activatedUser);
		loginMapper.updateUserLastLoginDtime(activatedUser.userCd());

		return LoginResponse.from(activatedUser, refreshToken, token);
	}

}

