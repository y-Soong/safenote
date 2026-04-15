package com.prafta.common.cmm.login.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import com.prafta.common.cmm.login.application.command.RequiredTermsInfoCommand;
import com.prafta.common.cmm.login.application.command.UserJoinCommand;
import com.prafta.common.cmm.login.application.command.UserLogoutCommand;
import com.prafta.common.cmm.login.application.command.UserPwdFailCommand;
import com.prafta.common.cmm.login.application.command.UserPwdUnlockCommand;
import com.prafta.common.cmm.login.application.param.AuthMenuInfoParam;
import com.prafta.common.cmm.login.application.param.LoginParam;
import com.prafta.common.cmm.login.application.param.LogoutParam;
import com.prafta.common.cmm.login.application.param.UserJoinParam;
import com.prafta.common.cmm.login.application.param.UserTermsAgreementCheckParam;
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
		    
		    // 잠금 해제 시각이 지났남, 계정 상태 업데이트
		    loginMapper.userPwdUnLock(UserPwdUnlockCommand.from(userResult));
		}
		
		String hashedPw = (String) userResult.userPw();
		
		if(!passwordHasher.matches(param.userPw(), hashedPw)) {
			
			loginMapper.updateUserPwdFail(UserPwdFailCommand.from(userResult, lockDurationMinutes, maxFailCount));
			
			throw new ApiException(LoginErrorCode.LOGIN_400_001);
		}

		// 사용자 정보 LOCK 잡기
		loginMapper.lockUserRow(UserRowLockQuery.from(userResult));
		
		String tokenId = UUID.randomUUID().toString().replace("-", "");
		
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[64]; // 64바이트면 충분히 강함
		random.nextBytes(bytes);
		String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		String refreshTokenHash = hmacSigner.hmacSha256Base64Url(refreshToken);
		
		ActiveTokenCommand activeTokenCommand = ActiveTokenCommand.from(userResult, tokenId, param.clientType(), refreshTokenHash, "7");
		
		// 기존 세션 revoke
		loginMapper.revokeActiveToken(activeTokenCommand);
		// 신규 세션 insert
		loginMapper.insertAuthToken(activeTokenCommand);
		
		String mblNo = aesGcmCrypto.decrypt(userResult.mblNoEnc());
		String email = aesGcmCrypto.decrypt(userResult.emailEnc());
		String token = jwtUtil.generateToken(userResult, mblNo, email);
		
		// 사용자 로그인 시간 기록
		loginMapper.updateUserLastLoginDtime(userResult.userCd());
				
		return LoginResponse.from(userResult, mblNo, email, refreshToken, token);
	}
	
	@Override
    @Transactional
    public void logout(LogoutParam param) {

        loginMapper.revokeAllActiveTokens(UserLogoutCommand.from(param));
        loginMapper.revokeActiveTokenByClient(UserLogoutCommand.from(param));
    }
	
	@Transactional
    public void insertUserInfo(UserJoinParam param) {
		
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
        String phoneHmac = (phoneNorm == null) ? null : hmacSigner.hmacSha256Base64Url(phoneNorm + ":" + param.cmpnyCd());
        String emailHmac = (emailNorm == null) ? null : hmacSigner.hmacSha256Base64Url(emailNorm + ":" + param.cmpnyCd());

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
	
	public static String sha256(String input) {
	    try {
	        MessageDigest md = MessageDigest.getInstance("SHA-256");
	        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
	        StringBuilder sb = new StringBuilder();
	        for (byte b : digest) sb.append(String.format("%02x", b));
	        return sb.toString();
	    } catch (NoSuchAlgorithmException e) {
	        throw new RuntimeException(e);
	    }
	}
}

