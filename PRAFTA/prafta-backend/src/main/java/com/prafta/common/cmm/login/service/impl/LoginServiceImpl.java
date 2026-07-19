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
import com.prafta.common.cmm.login.application.command.DeviceOccupancyAnomalyCommand;
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
	// PRAFTA-COM-008-E-8 — 기본 근무타입 로그인 게이트(교대 비소속 판정 + 설정 저장 + 즉시 생성).
	private final com.prafta.common.cmm.shift.service.ShiftMembershipService shiftMembershipService;
	private final com.prafta.common.cmm.sch.service.DefaultSchOptionService defaultSchOptionService;
	private final com.prafta.common.cmm.sch.service.DefaultSchGenService defaultSchGenService;
	private final com.prafta.common.cmm.sch.mapper.DefaultSchGenMapper defaultSchGenMapper;

	// PRAFTA-036 — 인증대기 분기에서 발급하는 임시 토큰 만료(분).
	private static final int PHONE_AUTH_TOKEN_TTL_MINUTES = 10;
	// PRAFTA-COM-008-E-8 — 기본 근무타입 게이트 임시 토큰 만료(분, PhoneAuth 미러).
	private static final int DEFAULT_SCH_TOKEN_TTL_MINUTES = 10;
	// 자동생성 트리거 운영 게이트. 코드 기본값 true 로 통일(User_01/배치와 정합). properties 명시값 우선.
	@Value("${prafta.default-sch.gen.enabled:true}")
	private boolean defaultSchGenEnabled;
	// 게이트 평가 시점 운영 토글(기본 true). 운영 검증 전 빠른 비활성화 경로.
	@Value("${prafta.default-sch.gate.enabled:true}")
	private boolean defaultSchGateEnabled;
	
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

		// PRAFTA-app-027-7: 비밀번호 인증 성공 — 실패 카운트/잠금 무조건 초기화(성공 시 0 복귀).
		//   진입 시 userPwdUnLock 은 만료 잠금에만 적용하므로 성공 경로에서 명시적으로 리셋한다.
		//   Login 은 @Transactional 아님 → auto-commit 으로 즉시 영속(실패 누적도 동일 정책).
		loginMapper.updateUserPwdReset(UserPwdUnlockCommand.from(userResult));

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

		// PRAFTA-COM-008-E-8: 기본 근무타입 로그인 게이트(D-E2: PHONE_AUTH 통과 후 평가).
		//   DEFAULT_SCH_CD 미설정 AND 교대팀 비소속이면 정식 토큰 대신 임시 scope=DEFAULT_SCH 토큰만 발급.
		//   서버 강제 — 클라 신뢰 금지. 기설정자/교대팀 소속자는 게이트 미발동(정상 로그인).
		if (requiresDefaultSchGate(userResult)) {
			String defaultSchToken = jwtUtil.generateScopeToken(
					userResult.cmpnyCd(), userResult.userCd(),
					com.prafta.common.security.JwtScope.DEFAULT_SCH, DEFAULT_SCH_TOKEN_TTL_MINUTES);
			log.info("기본 근무타입 미설정 로그인 — 게이트 임시 토큰 발급(scope=DEFAULT_SCH). userCd={}", userResult.userCd());
			return LoginResponse.defaultSchPending(userResult, defaultSchToken);
		}

		// 사용자 정보 LOCK 잡기
		loginMapper.lockUserRow(UserRowLockQuery.from(userResult));
		
		String tokenId = UUID.randomUUID().toString().replace("-", "");
		
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[64]; // 64바이트면 충분히 강함
		random.nextBytes(bytes);
		String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		String refreshTokenHash = hmacSigner.hmacSha256Base64Url(refreshToken);
		
		// prafta-057: 로그인 세션 패밀리 식별자(회전 시 승계 · 다른 환경 로그인 감지용)
		String loginId = UUID.randomUUID().toString().replace("-", "");
		// 정책 §3.4: Refresh Token 유효기간 48시간(2일)
		ActiveTokenCommand activeTokenCommand = ActiveTokenCommand.from(userResult, tokenId, loginId, param.clientType(), refreshTokenHash, "2");

		// 기존 세션 revoke
		loginMapper.revokeActiveToken(activeTokenCommand);
		// 신규 세션 insert
		loginMapper.insertAuthToken(activeTokenCommand);
		
		// 정책 §11.1에 따라 로그인 응답 페이로드에서 휴대폰/이메일 평문을 제거.
		// 필요한 화면은 인증된 별도 API(/webApi/user01/user-info-lists)로 조회한다.
		String token = jwtUtil.generateToken(userResult, loginId, param.clientType());

		// 사용자 로그인 시간 기록
		loginMapper.updateUserLastLoginDtime(userResult.cmpnyCd(), userResult.userCd());

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
			// prafta-com-015 015-1: 점유 재할당 이상탐지(upsert 직전).
			//   직전 점유자(USER_CD)가 "다른 계정" 이면 감사행 적재(재할당 자체는 막지 않음).
			//   동일 계정 재로그인(iOS IDFV 변경/재설치 포함)은 미적재.
			detectOccupancyAnomaly(userResult.cmpnyCd(), param.deviceId(), userResult.userCd(),
					param.clientType(), param.ipAddr());
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
			// (계정당 활성 디바이스 1대) 같은 사용자의 다른 기기를 비활성화 → 마지막 로그인 기기만 푸시 수신.
			//   stale 기기로의 푸시 누수/오발송을 막고, 디바이스 점유를 항상 최신으로 정리한다.
			int deactivated = loginMapper.deactivateOtherUserDevices(
					userResult.cmpnyCd(), userResult.userCd(), param.deviceId());
			// PII(기기ID/IP) 평문 로그 금지 — 식별 키만 남긴다.
			log.info("디바이스 로그인 이력 적재 완료 — userCd={}, clientType={}, 비활성화기기={}건"
					, userResult.userCd(), param.clientType(), deactivated);
		} catch (Exception e) {
			log.error("디바이스 로그인 이력 적재 실패(로그인 영향 없음) — userCd={}", userResult.userCd(), e);
		}
	}

	/**
	 * prafta-com-015 015-1 — 디바이스 점유 재할당 이상탐지(감사만, 차단 없음).
	 *
	 * <p>로그인 upsert 직전 해당 deviceUuid 의 현재 점유자(USER_CD)를 조회해, 점유자가 "다른 계정"
	 * 이면 이상행을 적재하고 log.warn 남긴다(R1). 점유자가 없거나(신규 기기) 동일 계정(iOS IDFV
	 * 변경/재설치 포함)이면 미적재. recordDeviceLogin 의 try-catch 안에서 호출되어 실패는 격리된다.
	 */
	private void detectOccupancyAnomaly(String cmpnyCd, String deviceUuid, String newUserCd,
			String clientType, String loginIp) {
		// 이상탐지/적재 실패(예: 마이그 미적용으로 Unknown table)가 뒤따르는 디바이스 upsert/이력/
		//   단일기기 정리(핵심 로직)를 막지 않도록 자체 격리한다. 탐지는 부가 기능이므로 실패해도 무시.
		try {
			String prevOwner = loginMapper.selectDeviceOwner(deviceUuid);
			if (prevOwner == null || prevOwner.equals(newUserCd)) {
				return; // 신규 기기 또는 동일 계정 재로그인 → 정상(이상 아님).
			}
			// 점유자 변경 — 감사행 적재. deviceUuid/userCd 는 마스킹 로그(PII/식별자 평문 금지).
			loginMapper.insertOccupancyAnomaly(new DeviceOccupancyAnomalyCommand(
					cmpnyCd, deviceUuid, prevOwner, newUserCd, clientType, loginIp));
			log.warn("디바이스 점유 재할당 감지 — deviceUuid={}, prevUserCd={}, newUserCd={}, clientType={}",
					maskHead(deviceUuid), maskHead(prevOwner), maskHead(newUserCd), clientType);
		} catch (Exception e) {
			log.warn("디바이스 점유 이상탐지 실패(무시, 디바이스 적재 계속) — deviceUuid={}: {}",
					maskHead(deviceUuid), e.getMessage());
		}
	}

	/** 로그 위조 방지용 외부 입력 정제 — 개행 제거 + 50자 상한 (subcon01 SEC-ADV-1 규약 미러). */
	private String sanitizeForLog(String value) {
		if (value == null) {
			return null;
		}
		String cleaned = value.trim().replaceAll("[\\r\\n]", "");
		return cleaned.length() > 50 ? cleaned.substring(0, 50) + "..." : cleaned;
	}

	/** 식별자 마스킹: 앞 8자 + ***(짧으면 길이만큼). 평문 로그 금지(S2 패턴 미러). */
	private String maskHead(String value) {
		if (value == null || value.isBlank()) {
			return "(none)";
		}
		int head = Math.min(8, value.length());
		return value.substring(0, head) + "***";
	}

	/**
	 * PRAFTA-COM-008-E-8 — 기본 근무타입 게이트 발동 여부.
	 *
	 * <p>오늘 기준 교대팀 비소속이면서 DEFAULT_SCH_CD 가 비어 있으면 게이트 발동(true).
	 * 운영 토글(prafta.default-sch.gate.enabled=false)이면 항상 비발동.
	 * 조회 실패 등 어떤 예외도 게이트를 발동시키지 않는다(fail-open) — 로그인 자체를 막지 않기 위함.
	 */
	private boolean requiresDefaultSchGate(UserResult userResult) {
		if (!defaultSchGateEnabled) {
			return false;
		}
		try {
			String cmpnyCd = userResult.cmpnyCd();
			String userCd = userResult.userCd();
			String siteCd = userResult.siteCd();
			if (siteCd == null || siteCd.isBlank()) {
				siteCd = defaultSchGenMapper.selectUserSiteCd(cmpnyCd, userCd);
			}
			if (siteCd == null || siteCd.isBlank()) {
				return false; // 사업장 미상 — 게이트 평가 불가, 정상 로그인 진행.
			}
			// 이미 설정됨 → 게이트 미발동. selectDefaultSchUser 는 DEFAULT_SCH_CD NOT NULL 일 때만 행 반환.
			if (defaultSchGenMapper.selectDefaultSchUser(cmpnyCd, userCd) != null) {
				return false;
			}
			// 교대팀 소속(오늘) → 교대패턴이 스케줄 보장 → 게이트 미발동.
			String today = java.time.LocalDate.now()
					.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
			if (shiftMembershipService.isInShiftTeamOn(cmpnyCd, siteCd, userCd, today)) {
				return false;
			}
			return true; // 미설정 + 교대 비소속 → 게이트 발동.
		} catch (Exception e) {
			log.error("기본 근무타입 게이트 판정 실패(로그인 영향 없음 — 게이트 미발동) — userCd={}",
					userResult.userCd(), e);
			return false;
		}
	}

	/**
	 * PRAFTA-COM-008-E-8 — 기본 근무타입 게이트 통과 처리.
	 *
	 * <p>scope=DEFAULT_SCH 임시 토큰의 claim(cmpnyCd/userCd)으로만 대상 식별(IDOR 방지).
	 * 화이트리스트 검증(대상 사용자 SITE_CD ∩ USE_YN='Y') → DEFAULT_SCH_CD 저장 + 설정시점 기록
	 * → 즉시 자동생성(게이트 enabled 시) → 정식 토큰/리프레시 발급(일반 로그인 동일).
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public LoginResponse setDefaultSch(String gvCmpnyCd, String gvUserCd, String defaultSchCd, String clientType) {

		if (defaultSchCd == null || defaultSchCd.isBlank()) {
			throw new ApiException(com.prafta.common.error.attd.AttdErrorCode.ATTD_400_141);
		}

		// 1) 대상 사용자 조회 + 상태 검증(탈퇴/인증대기 차단).
		UserResult userResult = loginMapper.selectUserByUserCd(gvCmpnyCd, gvUserCd);
		if (userResult == null) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}
		if ("03".equals(userResult.accountStatus())) {
			throw new ApiException(LoginErrorCode.LOGIN_400_014);
		}
		if ("04".equals(userResult.accountStatus())) {
			throw new ApiException(LoginErrorCode.LOGIN_400_013);
		}

		// 2) 검증 스코프 SITE_CD 도출(토큰 식별 사용자의 실제 소속).
		String siteCd = userResult.siteCd();
		if (siteCd == null || siteCd.isBlank()) {
			siteCd = defaultSchGenMapper.selectUserSiteCd(gvCmpnyCd, gvUserCd);
		}
		if (siteCd == null || siteCd.isBlank()) {
			throw new ApiException(com.prafta.common.error.attd.AttdErrorCode.ATTD_400_140);
		}

		// 3) 화이트리스트 검증(클라 제출값 신뢰 금지).
		if (!defaultSchOptionService.isValidDefaultSch(gvCmpnyCd, siteCd, defaultSchCd)) {
			throw new ApiException(com.prafta.common.error.attd.AttdErrorCode.ATTD_400_140);
		}

		// 4) DEFAULT_SCH_CD + 설정시점 저장.
		int updated = defaultSchGenMapper.updateUserDefaultSch(gvCmpnyCd, gvUserCd, defaultSchCd, gvUserCd);
		if (updated == 0) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}

		// 5) 즉시 자동생성(E-3 트리거2) — 운영 게이트 on 일 때만. 실패는 격리(로그인 차단 금지).
		if (defaultSchGenEnabled) {
			try {
				defaultSchGenService.applyDefaultSchChange(gvCmpnyCd, siteCd, gvUserCd, defaultSchCd);
			} catch (Exception e) {
				log.error("기본 근무타입 게이트 — 자동생성 실패(설정은 저장됨) — userCd={}", gvUserCd, e);
			}
		}

		log.info("기본 근무타입 게이트 통과 — userCd={}, siteCd={}, defaultSchCd={}", gvUserCd, siteCd, defaultSchCd);

		// 6) 정식 토큰/리프레시 발급(일반 로그인 흐름 동일).
		loginMapper.lockUserRow(UserRowLockQuery.from(userResult));

		String tokenId = UUID.randomUUID().toString().replace("-", "");
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[64];
		random.nextBytes(bytes);
		String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		String refreshTokenHash = hmacSigner.hmacSha256Base64Url(refreshToken);

		// prafta-057: 로그인 세션 패밀리 식별자(회전 시 승계 · 다른 환경 로그인 감지용)
		String loginId = UUID.randomUUID().toString().replace("-", "");
		ActiveTokenCommand activeTokenCommand = ActiveTokenCommand.from(userResult, tokenId, loginId, clientType, refreshTokenHash, "2");
		loginMapper.revokeActiveToken(activeTokenCommand);
		loginMapper.insertAuthToken(activeTokenCommand);

		String token = jwtUtil.generateToken(userResult, loginId, clientType);
		loginMapper.updateUserLastLoginDtime(userResult.cmpnyCd(), userResult.userCd());

		return LoginResponse.from(userResult, refreshToken, token);
	}

	@Override
    @Transactional
    public void logout(LogoutParam param) {

        loginMapper.revokeAllActiveTokens(UserLogoutCommand.from(param));
        loginMapper.revokeActiveTokenByClient(UserLogoutCommand.from(param));

        // prafta-com-015 015-3: 현재 기기 푸시 토큰 정리(stale 토큰 누수 차단).
        //   userCd/deviceId 가 모두 있을 때만(앱). 실패는 격리 — 로그아웃(토큰 revoke) 흐름 무영향.
        clearPushTokenOnLogout(param);
    }

    /**
     * prafta-com-015 015-3 — 로그아웃 시 현재 기기 PUSH_TOKEN 정리(best-effort).
     *
     * <p>userCd 와 deviceId 가 모두 유효할 때만 정리한다(웹/식별 불가 시 skip).
     * 어떤 예외가 나도 로그아웃(세션 revoke) 자체를 막지 않으며 log.warn 만 남긴다.
     */
    private void clearPushTokenOnLogout(LogoutParam param) {
        try {
            String userCd = param.userCd();
            String deviceId = param.deviceId();
            if (userCd == null || userCd.isBlank() || "SYSTEM".equals(userCd)
                    || deviceId == null || deviceId.isBlank()) {
                return; // 식별 불가(웹/토큰 없음) → 정리 대상 아님.
            }
            int cleared = loginMapper.clearPushTokenForDevice(param.cmpnyCd(), userCd, deviceId);
            log.info("로그아웃 시 푸시 토큰 정리 — deviceUuid={}, 영향행={}건", maskHead(deviceId), cleared);
        } catch (Exception e) {
            log.warn("로그아웃 시 푸시 토큰 정리 실패(로그아웃 영향 없음) — userCd={}", param.userCd(), e);
        }
    }
	
	@Transactional
    public void insertUserInfo(UserJoinParam param) {

		// 0) PRAFTA-046: 가입 대상 노드에 정/부 관리자가 존재해야 가입 가능 (fail-closed).
		//    노드 미존재/관리자 미지정이면 가입 거부 — 회사 내부구조 노출 없는 친화 메시지.
		int nodeHasAdmin = loginMapper.selectNodeHasAdmin(param.cmpnyCd(), param.siteCd(), param.nodeCd());
		if (nodeHasAdmin == 0) {
			throw new ApiException(LoginErrorCode.LOGIN_400_015);
		}

		// 0-0) PRAFTA-SUBCON-T2-07: 연동 미러 사업장 가입 차단(2중 가드 — join-site-lists 제외 + INSERT 직전 검증).
		//      목록 우회 직접 호출 방어. 거부 메시지는 미러 존재 사실을 상세 노출하지 않는다.
		if (loginMapper.selectMirrorSiteCnt(param.cmpnyCd(), param.siteCd()) > 0) {
			log.info("회원가입 차단 - 연동 미러 사업장 cmpnyCd={}, siteCd={}",
					sanitizeForLog(param.cmpnyCd()), sanitizeForLog(param.siteCd()));
			throw new ApiException(LoginErrorCode.LOGIN_400_017);
		}

		// 0-1) 로그인 ID 중복 검사(전사 — 일용직 포함).
		//      로그인은 USER_ID 만으로 사용자를 찾으므로 ID 는 전역 유일해야 한다. 서버측 사전검사가 없으면
		//      DB UNIQUE(UX_TB_USER_ID) 위반이 500 으로 새어나간다.
		if (loginMapper.selectUserIdExistsGlobal(param.userId()) > 0) {
			throw new ApiException(LoginErrorCode.LOGIN_400_016);
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
				loginMapper.mergeAuthMenuInfo(AuthMenuInfoCommand.from(Param.cmpnyCd(), systValDCd, Param.userId()));
			}
		}
	}

	/**
	 * 휴대폰 본인인증 팝업 자동기입용 — PHONE_AUTH 토큰 식별 사용자(인증대기 '04')의
	 * 관리자 등록 휴대폰을 복호화하여 반환한다. 클라가 임의 번호로 바꿔 계정 휴대폰을 탈취하는 것을 막기 위함.
	 * 대상이 '04' 가 아니거나 미존재면 거부, 미등록(enc 없음)이면 빈 문자열(수동 입력 폴백).
	 */
	@Override
	public String getPhoneAuthTargetPhone(String cmpnyCd, String userCd) {
		UserResult userResult = loginMapper.selectUserByUserCd(cmpnyCd, userCd);
		if (userResult == null || !"04".equals(userResult.accountStatus())) {
			throw new ApiException(LoginErrorCode.LOGIN_400_013);
		}
		String enc = userResult.mblNoEnc();
		return (enc == null || enc.isBlank()) ? "" : aesGcmCrypto.decrypt(enc);
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

		// PRAFTA-COM-008-E-8: 인증대기('04') 활성화 직후에도 기본 근무타입 게이트를 재평가한다.
		//   인증대기 계정은 main login() 에서 PHONE_AUTH 로 early-return 하여 DEFAULT_SCH 게이트를 거치지 못한다.
		//   여기서 재평가하지 않으면 신규 고객사(프로비저닝) 계정의 최초 로그인에서 게이트가 영구 우회되어
		//   DEFAULT_SCH_CD 미설정 상태로 메인에 진입한다. main login() 과 동일 규칙으로 정식 토큰 대신
		//   scope=DEFAULT_SCH 임시 토큰을 발급하고, 클라이언트는 게이트 팝업으로 분기한다.
		if (requiresDefaultSchGate(activatedUser)) {
			String defaultSchToken = jwtUtil.generateScopeToken(
					activatedUser.cmpnyCd(), activatedUser.userCd(),
					com.prafta.common.security.JwtScope.DEFAULT_SCH, DEFAULT_SCH_TOKEN_TTL_MINUTES);
			log.info("인증대기 활성화 후 기본 근무타입 미설정 — 게이트 임시 토큰 발급(scope=DEFAULT_SCH). userCd={}",
					activatedUser.userCd());
			return LoginResponse.defaultSchPending(activatedUser, defaultSchToken);
		}

		String tokenId = java.util.UUID.randomUUID().toString().replace("-", "");
		java.security.SecureRandom random = new java.security.SecureRandom();
		byte[] bytes = new byte[64];
		random.nextBytes(bytes);
		String refreshToken = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		String refreshTokenHash = hmacSigner.hmacSha256Base64Url(refreshToken);

		// prafta-057: 로그인 세션 패밀리 식별자(회전 시 승계 · 다른 환경 로그인 감지용)
		String loginId = java.util.UUID.randomUUID().toString().replace("-", "");
		ActiveTokenCommand activeTokenCommand = ActiveTokenCommand.from(
				activatedUser, tokenId, loginId, param.clientType(), refreshTokenHash, "2");
		loginMapper.revokeActiveToken(activeTokenCommand);
		loginMapper.insertAuthToken(activeTokenCommand);

		String token = jwtUtil.generateToken(activatedUser, loginId, param.clientType());
		loginMapper.updateUserLastLoginDtime(activatedUser.cmpnyCd(), activatedUser.userCd());

		return LoginResponse.from(activatedUser, refreshToken, token);
	}

}

