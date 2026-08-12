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
import com.prafta.common.cmm.audit.AuditActionType;
import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.cmm.audit.AuditResourceType;
import com.prafta.common.cmm.audit.command.AuditLogCommand;
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
import com.prafta.common.cmm.login.dto.response.UserJoinResponse;
import com.prafta.common.cmm.login.dto.response.UserTermsAgreementCheckResponse;
import com.prafta.common.cmm.login.mapper.LoginMapper;
import com.prafta.common.cmm.login.result.JoinConflictResult;
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
	// [security M-1] 셀프가입 재활용(거부 계정 부활) 감사 적재. 내부 REQUIRES_NEW + 예외 격리라 가입을 막지 않는다.
	private final com.prafta.common.cmm.audit.service.AuditLogService auditLogService;
	// PRAFTA-COM-008-E-8 — 기본 근무타입 로그인 게이트(교대 비소속 판정 + 설정 저장 + 즉시 생성).
	private final com.prafta.common.cmm.shift.service.ShiftMembershipService shiftMembershipService;
	private final com.prafta.common.cmm.sch.service.DefaultSchOptionService defaultSchOptionService;
	private final com.prafta.common.cmm.sch.service.DefaultSchGenService defaultSchGenService;
	private final com.prafta.common.cmm.sch.mapper.DefaultSchGenMapper defaultSchGenMapper;

	// PRAFTA-036 — 인증대기 분기에서 발급하는 임시 토큰 만료(분).
	private static final int PHONE_AUTH_TOKEN_TTL_MINUTES = 10;

	/**
	 * 소정-04 — 계정상태[SYS013] '06 가입승인대기' (셀프가입 직후, 관리자 승인 전).
	 *
	 * <p>★plan §1.5 는 '05' 로 적었으나 실DB SYS013 에 '05 비활성화'가 이미 있어 '06/07'로 확정됐다
	 * (마이그레이션 {@code sojeong-1-5-sys013-selfjoin-status-seed.sql} 참조).
	 */
	private static final String ACCOUNT_STATUS_JOIN_PENDING = "06";

	/** 소정-04 — 계정상태[SYS013] '07 가입거부' (USE_YN='N' 보존, 재가입 시 행 재활용). */
	private static final String ACCOUNT_STATUS_JOIN_REJECTED = "07";
	// PRAFTA-COM-008-E-8 — 기본 근무타입 게이트 임시 토큰 만료(분, PhoneAuth 미러).
	private static final int DEFAULT_SCH_TOKEN_TTL_MINUTES = 10;
	// 자동생성 트리거 운영 게이트. 코드 기본값 true 로 통일(User_01/배치와 정합). properties 명시값 우선.
	@Value("${prafta.default-sch.gen.enabled:true}")
	private boolean defaultSchGenEnabled;
	// 게이트 평가 시점 운영 토글(기본 true). 운영 검증 전 빠른 비활성화 경로.
	@Value("${prafta.default-sch.gate.enabled:true}")
	private boolean defaultSchGateEnabled;
	
	// [security H-1] 셀프가입 휴대폰 본인인증 서버 강제 토글(기본 on).
	//   ★재가입(거부 행 재활용) 경로에는 적용되지 않는다 — 그쪽은 항상 강제한다.
	@Value("${prafta.self-join.sms-verify.enabled:true}")
	private boolean selfJoinSmsVerifyEnabled;

	@Value("${login.lock.duration-minutes}")
	private int lockDurationMinutes;

	@Value("${login.lock.max-fail-count}")
	private int maxFailCount;
	
	public LoginResponse Login(LoginParam param) {

		UserResult userResult = loginMapper.Login(LoginQuery.from(param));

		if (userResult == null) {
			// 소정-04: 승인대기('06')·거부('07') 계정은 USE_YN='N' 이라 Login 쿼리에 잡히지 않는다.
			//   비밀번호가 맞는 경우에만 상태 안내를 노출한다(계정 존재 탐색 방지) — 아래 헬퍼 참조.
			assertNotPendingOrRejectedSelfJoin(param);
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
		// 소정-04: 셀프가입 승인대기('06') — 관리자 승인 전에는 어떤 토큰도 발급하지 않는다.
		//   '04'(휴대폰 인증대기)와 달리 임시 scope 토큰조차 주지 않는다. 셀프가입자는 본인인증이
		//   아니라 "관리자 승인"이라는 조직 판단을 기다리는 상태라 클라이언트가 스스로 통과시킬
		//   경로가 없어야 하기 때문이다(승인은 User_09 관리자 화면에서만 일어난다).
		if (ACCOUNT_STATUS_JOIN_PENDING.equals(accountStatus)) {
			log.info("가입 승인대기 계정 로그인 시도 — 토큰 미발급. userCd={}", userResult.userCd());
			throw new ApiException(LoginErrorCode.LOGIN_400_018);
		}
		// 소정-04: 거부('07')는 USE_YN='N' 이라 원칙적으로 여기 도달하지 않지만(Login 쿼리에서 제외),
		//   사용여부만 되살아나는 운영 조작에 대비해 fail-closed 로 한 번 더 막는다.
		if (ACCOUNT_STATUS_JOIN_REJECTED.equals(accountStatus)) {
			log.info("가입 거부 계정 로그인 시도 — 차단. userCd={}", userResult.userCd());
			throw new ApiException(LoginErrorCode.LOGIN_400_019);
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
	 * 소정-04 — 승인대기('06')·거부('07') 계정 로그인 시도에 대한 안내 분기.
	 *
	 * <p>두 상태 모두 {@code USE_YN='N'}(security M-2: 미승인자의 재직자 목록 유입 차단)이라
	 * {@code Login} 쿼리가 행을 반환하지 않아, 그대로 두면 "아이디 혹은 비밀번호를 확인해주세요"만
	 * 뜬다. 승인 대기 중이라는 사실이나 재가입 가능 여부를 알려주지 않으면 사용자는 같은 아이디로
	 * 무한 재시도하게 되므로 전용 안내를 준다.
	 *
	 * <p><b>노출 조건 = 비밀번호 일치</b>. 아이디만으로 "승인대기/거부 계정"이라고 알려주면 계정
	 * 존재 여부 탐색(enumeration)이 되므로, 본인만 아는 비밀번호를 맞힌 경우에만 안내한다.
	 * 비밀번호가 틀리면 실패 카운트를 누적하고(정규 경로 미러) 통합 메시지로 되돌린다.
	 *
	 * <p>어떤 경우에도 토큰을 발급하지 않는다(계정은 여전히 비활성).
	 */
	private void assertNotPendingOrRejectedSelfJoin(LoginParam param) {

		UserResult inactive = loginMapper.selectSelfJoinInactiveUserByUserId(param.userId());
		if (inactive == null) {
			return; // 승인대기/거부 계정이 아님 → 호출부의 통합 차단 메시지로 진행.
		}

		// 잠금 중이면 비밀번호 검증 없이 통합 메시지(대입 시도 차단).
		if ("Y".equals(inactive.pwdLockYn())) {
			String unlockDtimeStr = inactive.pwdLockExpireDtime();
			if (unlockDtimeStr != null) {
				LocalDateTime unlockDtime = LocalDateTime.parse(unlockDtimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
				if (LocalDateTime.now().isBefore(unlockDtime)) {
					return;
				}
			}
		}

		if (!passwordHasher.matches(param.userPw(), inactive.userPw())) {
			// 정규 경로와 동일하게 실패를 누적한다(비활성 계정을 무제한 비번 대입 창구로 쓰지 못하게).
			loginMapper.updateUserPwdFail(UserPwdFailCommand.from(inactive, lockDurationMinutes, maxFailCount));
			return;
		}

		if (ACCOUNT_STATUS_JOIN_PENDING.equals(inactive.accountStatus())) {
			log.info("가입 승인대기 계정 로그인 시도(비밀번호 일치) — 승인대기 안내. userCd={}", inactive.userCd());
			throw new ApiException(LoginErrorCode.LOGIN_400_018);
		}

		log.info("가입 거부 계정 로그인 시도(비밀번호 일치) — 재가입 안내. userCd={}", inactive.userCd());
		throw new ApiException(LoginErrorCode.LOGIN_400_019);
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
	
	/**
	 * 셀프가입(회원가입) 접수.
	 *
	 * <p>★소정-04 이후 이 메서드는 <b>계정을 활성화하지 않는다</b>. ACCOUNT_STATUS='06'(가입승인대기),
	 * USE_YN='N' 으로 적재하고, 관리자가 User_09 에서 승인해야 '01' + USE_YN='Y' 가 된다.
	 * 거부('07') 행이 있으면 그 행을 재활용한다.
	 *
	 * <p>★★[security H-1] 진입부에서 <b>서버측 휴대폰 본인인증</b>을 강제한다. 클라이언트 화면이
	 * 인증 후에만 가입 버튼을 열어주는 것은 방어가 아니다(EP 직접 호출로 우회 가능).
	 */
	@Override
	@Transactional
    public UserJoinResponse insertUserInfo(UserJoinParam param, AuditContext auditContext) {

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

        // 5-1) 소정-04: 아이디/휴대폰 중복 판정 + 거부('07') 행 재활용 대상 해석.
        //      ★기존 중복 검사(LOGIN_400_016)를 대체하는 지점이다. 재활용 대상이 아니면 종전대로 차단한다.
        JoinConflictResult rejoinTarget = resolveRejoinTarget(param, phoneHmac);

        // 5-2) ★★[security H-1] 휴대폰 본인인증 서버 강제 — 쓰기 직전 fail-closed.
        //      재활용(재가입) 경로는 계정 선점 공격이 성립하는 지점이라 토글과 무관하게 항상 강제한다.
        assertPhoneOwnershipVerified(param, phoneHmac, rejoinTarget != null);

        String userCd = (rejoinTarget != null)
                ? rejoinTarget.userCd()                      // 재가입: 기존 '07' 행의 USER_CD 를 그대로 쓴다.
                : loginMapper.selectUserCd(param.cmpnyCd()); // 신규 가입: 신규 채번.

        UserJoinCommand userJoinCommand = UserJoinCommand.from(param, userCd, userPw, phoneEnc, phoneHmac, phoneLast4, emailEnc, emailHmac, emailDomain, birthEnc);

        // 6) INSERT (신규) 또는 거부 행 재활용 UPDATE (재가입). 둘 다 ACCOUNT_STATUS='06'(가입승인대기).
        if (rejoinTarget != null) {
            int revived = loginMapper.updateRejectedUserForRejoin(userJoinCommand);
            if (revived == 0) {
                // 동시성: 판정과 갱신 사이에 상태가 바뀜(다른 요청이 먼저 재활용). 재시도를 안내한다.
                log.info("재가입 재활용 실패(대상 상태 변경) - cmpnyCd={}, userCd={}",
                        sanitizeForLog(param.cmpnyCd()), maskHead(userCd));
                throw new ApiException(LoginErrorCode.LOGIN_400_016);
            }
            // 이전 신청 사업장 권한 회수 — 다른 사업장으로 재신청한 경우 승인 후 옛 사업장 데이터까지
            //   접근하게 되는 것을 막는다(SiteAccessService 가 이 원장을 본다).
            int revoked = loginMapper.deactivateOtherUserSiteAuth(param.cmpnyCd(), userCd, param.siteCd());
            log.info("셀프가입 재신청(거부 계정 재활용) - cmpnyCd={}, userCd={}, 상태 07→06, 이전 사업장권한 회수={}건",
                    sanitizeForLog(param.cmpnyCd()), maskHead(userCd), revoked);

            // [security M-1] 재활용은 "거부된 계정이 되살아나는" 상태 전이다. 이 사실이 어디에도
            //   남지 않으면 거부 이력(감사 로그)만 있고 그 뒤 무슨 일이 있었는지 추적할 수 없다.
            //   비로그인 경로라 행위자 식별이 불가능하므로 userCd 에는 대상 계정을 넣고 IP/UA 로 보완한다.
            auditLogService.record(AuditLogCommand.builder()
                    .cmpnyCd(param.cmpnyCd())
                    .userCd(userCd)
                    .actionType(AuditActionType.STATUS_CHANGE)
                    .resourceType(AuditResourceType.SELF_JOIN_APPROVAL)
                    .resourceKey(userCd)
                    .detailJson("{\"action\":\"REJOIN\",\"from\":\"07\",\"to\":\"06\"}")
                    .build(), auditContext);
        } else {
            loginMapper.insertUserInfo(userJoinCommand);
            log.info("셀프가입 신청 접수 - cmpnyCd={}, userCd={}, 상태=06(가입승인대기)",
                    sanitizeForLog(param.cmpnyCd()), maskHead(userCd));
        }
        loginMapper.insertUserSiteAuth(userJoinCommand);

        // 약관 동의 로직은 그대로
        List<RequiredTermsResult> RequiredTermsResultList = loginMapper.selectRequiredTermsList();
        if (RequiredTermsResultList == null || RequiredTermsResultList.isEmpty()) {
            throw new ApiException(LoginErrorCode.LOGIN_500_001);
        }
        for (RequiredTermsResult RequiredTermsResult : RequiredTermsResultList) {

            loginMapper.insertTermsUserAgrMgmt(RequiredTermsInfoCommand.from(userCd, param, RequiredTermsResult));
        }

        // 클라이언트가 "로그인 해주세요" 대신 승인대기 안내로 분기하도록 상태 신호를 돌려준다.
        //   (앱 utils/joinApproval.js 계약 — nextStep / accountStatus 어느 쪽을 봐도 동일 결론)
        return UserJoinResponse.pending();
    }
	
	/**
	 * ★★[security H-1] 셀프가입 휴대폰 본인인증 서버 강제 (fail-closed).
	 *
	 * <p><b>무엇이 문제였나</b> — 가입 EP 는 {@code @NoAuth} 공개 경로인데 서버가 SMS 인증 여부를
	 * 전혀 보지 않았다. 화면이 "인증 완료 후 가입 버튼 활성화"로 막고 있을 뿐이라, EP 를 직접
	 * 호출하면 <b>타인의 휴대폰번호로 가입</b>할 수 있었다. 재가입(행 재활용)과 결합하면 더 심각하다:
	 * 피해자 아이디가 거부('07')된 뒤 공격자가 자기 비밀번호로 그 행을 차지하면, 피해자는 이후
	 * 재가입이 영구 차단(400_016)되고 관리자 화면에는 공격자 입력값만 보여 식별조차 안 된다.
	 *
	 * <p><b>어떻게 막나</b> — 클라이언트가 이미 호출하는 {@code /comApi/baseinfo/sms-auth-checks} 가
	 * 성공하면 TB_SMS_AUTH_CODE 에 {@code VERIFIED_YN='Y'}(PURPOSE_CD='SELF_JOIN') 기록이 남는다.
	 * 가입 시점에 <b>제출된 휴대폰의 HMAC 으로 그 기록을 직접 조회</b>하고, 있으면 소비('C')한다.
	 * 즉 "그 번호로 실제 인증을 통과했음"을 서버가 확인한다. 인증번호(certNo) 재전송을 요구하지
	 * 않으므로 <b>기존 앱·웹 가입 화면의 요청 페이로드를 바꾸지 않는다</b>(회귀 없음).
	 *
	 * <p><b>소비('C')하는 이유</b> — 한 번의 인증으로 계정을 여러 개 만드는 것을 막는다.
	 * 소비 UPDATE 의 {@code WHERE VERIFIED_YN='Y'} 가 동시 요청 경합까지 닫는다(영향행 1건만 통과).
	 *
	 * <p><b>운영 토글</b> — {@code prafta.self-join.sms-verify.enabled}(기본 true). 예상 못한
	 * 구버전 클라이언트 형상이 발견될 때만 임시로 내리는 escape hatch 이며,
	 * <b>재활용(재가입) 경로에는 적용되지 않는다</b>(항상 강제 — 계정 선점 공격의 성립 지점이라
	 * 여기서 예외를 두면 취약점이 그대로 남는다).
	 *
	 * @param forceRequired 재활용(재가입) 경로 여부 — true 면 토글과 무관하게 강제
	 */
	private void assertPhoneOwnershipVerified(UserJoinParam param, String phoneHmac, boolean forceRequired) {

		if (phoneHmac == null) {
			// 휴대폰이 없으면 본인인증 자체가 성립하지 않는다(가입 필수 입력이기도 하다).
			throw new ApiException(LoginErrorCode.LOGIN_400_022);
		}

		String smsId = loginMapper.selectSelfJoinVerifiedSmsId(phoneHmac, param.certNo());
		if (smsId == null) {
			if (!forceRequired && !selfJoinSmsVerifyEnabled) {
				// 운영 토글로 내린 상태 — 그대로 통과시키되 흔적은 반드시 남긴다(무음 우회 금지).
				log.warn("셀프가입 휴대폰 본인인증 미확인 — 운영 토글(prafta.self-join.sms-verify.enabled=false)로 통과. cmpnyCd={}",
						sanitizeForLog(param.cmpnyCd()));
				return;
			}
			log.warn("셀프가입 차단 — 휴대폰 본인인증 기록 없음(또는 인증 창 만료). cmpnyCd={}, 재가입경로={}",
					sanitizeForLog(param.cmpnyCd()), forceRequired);
			throw new ApiException(LoginErrorCode.LOGIN_400_022);
		}

		if (loginMapper.consumeSelfJoinSmsAuth(smsId) != 1) {
			// 동시 요청이 먼저 소비함 → 재인증을 요구한다(같은 인증의 중복 사용 차단).
			log.warn("셀프가입 차단 — 휴대폰 본인인증 기록 소비 실패(중복 사용). cmpnyCd={}",
					sanitizeForLog(param.cmpnyCd()));
			throw new ApiException(LoginErrorCode.LOGIN_400_022);
		}
	}

	/**
	 * 소정-04 — 셀프가입 아이디/휴대폰 점유 판정 + 거부('07') 행 재활용 대상 해석.
	 *
	 * <p><b>왜 필요한가</b> — 거부는 계정을 지우지 않고 {@code '07' + USE_YN='N'} 로 보존한다
	 * (감사·중복 신청 판별). 그런데 {@code UX_TB_USER_ID}(USER_ID 전역 유일)와
	 * {@code UX_TB_USER_MBL_NO}(CMPNY_CD+MBL_NO_HMAC) 때문에 같은 아이디·휴대폰으로 신규 INSERT 를
	 * 하면 제약 위반이 난다. 사용자 확정 요건은 "같은 아이디/휴대폰으로 재가입 가능"이므로
	 * <b>그 '07' 행을 재활용</b>한다(plan §8 Q2).
	 *
	 * <p><b>판정식</b> (요청 회사 = {@code param.cmpnyCd()})
	 * <pre>
	 *   byId    = USER_ID 를 점유한 TB_USER 행 (전역)
	 *   byPhone = (회사, 휴대폰 HMAC) 를 점유한 TB_USER 행
	 *
	 *   byId 가 있고  (같은 회사 &amp;&amp; '07') 아니면          → 아이디 중복 차단 (400_016)
	 *   byPhone 이 있고 '07' 아니면                        → 휴대폰 중복 차단 (400_020)
	 *   byId·byPhone 둘 다 있고 USER_CD 가 다르면            → 병합 불가 차단 (400_021)
	 *   일용직(TB_DAILY_USER) 이 같은 ID 점유 중이면          → 아이디 중복 차단 (400_016)
	 *   그 외: byId ?: byPhone (둘 다 없으면 null = 신규 가입)
	 * </pre>
	 *
	 * <p>※ 휴대폰 중복은 종전에 사전 검사가 없어 UNIQUE 위반이 500 으로 새어나갔다. 재활용 판정을
	 * 위해 어차피 조회하므로 이 기회에 친화 메시지(400_020)로 바꾼다(차단 대상은 종전과 동일).
	 *
	 * @return 재활용할 '07' 행. 신규 가입이면 null
	 */
	private JoinConflictResult resolveRejoinTarget(UserJoinParam param, String phoneHmac) {

		// 일용직 ID 점유는 재활용 대상이 아니다(별 계통). 전역 유일 규칙 유지.
		if (loginMapper.selectDailyUserIdExists(param.userId()) > 0) {
			throw new ApiException(LoginErrorCode.LOGIN_400_016);
		}

		JoinConflictResult byId = loginMapper.selectJoinConflictByUserId(param.userId());
		if (byId != null && !isRejoinable(byId, param.cmpnyCd())) {
			throw new ApiException(LoginErrorCode.LOGIN_400_016);
		}

		JoinConflictResult byPhone = (phoneHmac == null)
				? null
				: loginMapper.selectJoinConflictByMblHmac(param.cmpnyCd(), phoneHmac);
		if (byPhone != null && !isRejoinable(byPhone, param.cmpnyCd())) {
			throw new ApiException(LoginErrorCode.LOGIN_400_020);
		}

		// 아이디와 휴대폰이 서로 다른 '07' 행을 가리키면 두 행을 하나로 합칠 수 없다.
		//   한쪽만 살리면 남은 행의 UNIQUE 가 여전히 충돌하므로 관리자 개입으로 넘긴다.
		if (byId != null && byPhone != null && !byId.userCd().equals(byPhone.userCd())) {
			log.info("재가입 재활용 불가(아이디/휴대폰이 서로 다른 거부 행에 귀속) - cmpnyCd={}",
					sanitizeForLog(param.cmpnyCd()));
			throw new ApiException(LoginErrorCode.LOGIN_400_021);
		}

		return (byId != null) ? byId : byPhone;
	}

	/** 재활용 가능한 점유 행인지 — 같은 회사의 '07 가입거부' 행만 해당된다. */
	private boolean isRejoinable(JoinConflictResult conflict, String cmpnyCd) {
		return ACCOUNT_STATUS_JOIN_REJECTED.equals(conflict.accountStatus())
				&& conflict.cmpnyCd() != null
				&& conflict.cmpnyCd().equals(cmpnyCd);
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
	 *
	 * <p>★★[4차 / qa R-2] {@code noRollbackFor = ApiException.class} 는 <b>필수다. 절대 제거하지 말 것.</b>
	 *    {@link BaseinfoService#userSmsAuthCheck} 와 {@code SmsVerifyGuard} 는 의도적으로 무트랜잭션이라
	 *    <u>이 메서드의 트랜잭션에 참여</u>한다. 그 안에서 일어나는 쓰기는
	 *    <ul>
	 *      <li>{@code increaseSelfJoinSmsFailCnt} — 인증번호 대입 카운터(sec C-2 / N-4)</li>
	 *      <li>{@code insertFailedVerifyAttempt} — 시간당 실패 시도 상한 재료(sec N-3 / T-2)</li>
	 *      <li>{@code resetExpiredVerifyLock} — 만료 잠금 해제(sec N-2)</li>
	 *    </ul>
	 *    전부 <b>방어 기록</b>이다. {@code rollbackFor = Exception.class} 만 두면 인증번호가 틀렸을 때
	 *    {@code ApiException} 전파로 이 기록들이 <b>통째로 롤백</b>되어, 이 EP 에서만 6자리 무제한 대입이
	 *    가능해진다(계정 활성화 = 계정 탈취).
	 *    <p>★<b>이 결함은 실동작 테스트로 잡히지 않는다</b> — 인증에 성공하면 정상 커밋되므로 증상이 없고,
	 *    실패했을 때만 조용히 카운터가 0 으로 되돌아간다. <b>코드 검사가 유일한 발견 수단이다.</b>
	 *    <p>선례: {@code PlatformLocationServiceImpl.verifySmsAuth} 가 정확히 같은 사유로 이미 이 조합을 쓴다.
	 *    <p><b>부분 커밋 부작용 전수 점검(4차)</b> — {@code ApiException} 이 던져질 수 있는 지점과 그 앞의 쓰기:
	 *    <ul>
	 *      <li>1)~4) 인증 실패 계열 — 앞선 쓰기는 위 방어 기록 3종뿐이다. <b>커밋되어야 하는 것들이다.</b></li>
	 *      <li>6) {@code activated == 0}({@code LOGIN_400_013}) — 앞서 {@code updateUserMblNo} 가 커밋된다.
	 *          사용자가 SMS 로 <b>소유를 증명한</b> 번호이고 {@code selectUserMblHmacConflict} 로 타인 보유가 아님을
	 *          이미 확인했으므로 안전하다(계정 상태는 그대로 '04' 라 재시도로 이어진다).</li>
	 *      <li>7) {@code activatedUser == null}({@code LOGIN_400_002}) — 앞서 활성화('01')가 커밋된다.
	 *          이 경우 사용자는 일반 로그인으로 진입할 수 있어 롤백보다 오히려 낫다.</li>
	 *    </ul>
	 *    ★{@code ApiException} 이 아닌 {@code RuntimeException} 은 여전히 전체 롤백된다({@code rollbackFor}).
	 */
	@Override
	@Transactional(rollbackFor = Exception.class, noRollbackFor = ApiException.class)
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

