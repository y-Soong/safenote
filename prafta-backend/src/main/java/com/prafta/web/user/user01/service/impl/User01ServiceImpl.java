package com.prafta.web.user.user01.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.login.LoginErrorCode;
import com.prafta.common.error.user.UserErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.common.util.PasswordHasher;
import com.prafta.web.baim.baim06.service.Baim06Service;
import com.prafta.web.user.user01.application.command.ScheduleWithdrawalCommand;
import com.prafta.web.user.user01.application.command.UserInfoCommand;
import com.prafta.web.user.user01.application.command.UserPasswdCommand;
import com.prafta.web.user.user01.application.command.UserSiteAuthCommand;
import com.prafta.web.user.user01.application.command.WithdrawMyAccountCommand;
import com.prafta.web.user.user01.application.model.UserInfoModel;
import com.prafta.web.user.user01.application.param.MyPasswdParam;
import com.prafta.web.user.user01.application.param.ScheduleWithdrawalParam;
import com.prafta.web.user.user01.application.param.SiteNodeAdminCandidateListParam;
import com.prafta.web.user.user01.application.param.WithdrawMyAccountParam;
import com.prafta.web.user.user01.application.param.UserInfoListParam;
import com.prafta.web.user.user01.application.param.UserPasswdParam;
import com.prafta.web.user.user01.application.query.SiteNodeAdminCandidateListQuery;
import com.prafta.web.user.user01.application.query.UserInfoListQuery;
import com.prafta.web.user.user01.application.query.UserNodeAdminCheckQuery;
import com.prafta.web.user.user01.application.query.UserSiteInfoQuery;
import com.prafta.web.user.user01.dto.response.SiteNodeAdminCandidateListResponse;
import com.prafta.web.user.user01.dto.response.UserInfoListResponse;
import com.prafta.web.user.user01.mapper.User01Mapper;
import com.prafta.web.user.user01.result.UserInfoResult;
import com.prafta.web.user.user01.result.UserPwResult;
import com.prafta.web.user.user01.result.UserSiteInfoResult;
import com.prafta.web.user.user01.service.User01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class User01ServiceImpl implements User01Service{
	private final User01Mapper user01Mapper;
	private final PasswordHasher passwordHasher;
	private final HmacSigner hmacSigner; 
	private final AesGcmCrypto aesGcmCrypto;
	
	private static final int PW_MIN_LEN = 6;
	private static final int PW_MAX_LEN = 15;

		
	public UserInfoListResponse selectUserInfoList(UserInfoListParam param) {
		
		UserInfoListResponse response = null;
		
		List<UserInfoResult> userInfoList = user01Mapper.selectUserInfoList(UserInfoListQuery.from(param));
		
		if(userInfoList != null && userInfoList.size() > 0) {
			response = UserInfoListResponse.builder()
						.userInfoList(userInfoList)
						.build();
		}
		
		return response;
	}
	
	public int updateUserPw(UserPasswdParam param) {

		String userPw = passwordHasher.hash(param.userCd());

		return user01Mapper.updateUserPw(UserPasswdCommand.from(param, userPw));
	}
	
	
	private static boolean isValidPassword(String password) {
		
	    if (password == null) return false;

	    int len = password.length();
	    if (len < PW_MIN_LEN || len > PW_MAX_LEN) return false;

	    int typeCount = 0;
	    if (password.matches(".*[0-9].*"))            typeCount++;
	    if (password.matches(".*[a-zA-Z].*"))         typeCount++;
	    if (password.matches(".*[^a-zA-Z0-9].*"))     typeCount++;
	    
	    return typeCount >= 2;
	}

	@Override
	public void updateMyPw(MyPasswdParam param) {

		// 1. Look up user by userId
		UserPwResult userPwResult = user01Mapper.selectUserPwByUserId(param.cmpnyCd(), param.userCd());
		if (userPwResult == null) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}

		// 2. Verify current password
		if (!passwordHasher.matches(param.currentPw(), userPwResult.userPw())) {
			throw new ApiException(UserErrorCode.USER_400_003);
		}

		// 3. Validate new password policy
		if (param.newPw() == null || !isValidPassword(param.newPw())) {
			throw new ApiException(UserErrorCode.USER_400_004);
		}

		// 4. Hash and update
		String newHash = passwordHasher.hash(param.newPw());
		user01Mapper.updateMyPw(
			new UserPasswdCommand(param.cmpnyCd(), userPwResult.userCd(), newHash)
		);
	}
	
	@Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateOneUserInfo(UserInfoModel model) {
		if(
			(model.nodeCd() != null && model.oriNodeCd() != null && !model.nodeCd().equals(model.oriNodeCd()))
			|| (model.useYn().equals("N"))
		) {
			
			int userNodeAdminCheck = user01Mapper.selectUserNodeAdminCheck(UserNodeAdminCheckQuery.from(model));
			
			if(userNodeAdminCheck > 0) {
				if(model.useYn().equals("N")) {
					throw new ApiException(UserErrorCode.USER_400_002);
				} else {
					System.out.println("UserErrorCode.USER_400_001 ####");
					throw new ApiException(UserErrorCode.USER_400_001);
				}
			}			
		}

        UserSiteInfoResult userInfoResult = user01Mapper.selectUserSiteInfo(UserSiteInfoQuery.from(model));

        if (userInfoResult == null) {
            throw new ApiException(LoginErrorCode.LOGIN_400_002);
        }

        boolean isMaster = "master".equals(model.authCd());
        String reqSiteCd = model.siteCd();
        String curSiteCd = userInfoResult.siteCd();
        
        String phoneNorm = "";
        String emailNorm = Normalizers.normalizeEmail(model.email());
        String birthNorm = Normalizers.normalizeBirth(model.birthDt());

        String phoneEnc = "";
        String emailEnc = "";
        String birthEnc = "";

        String phoneHmac = "";
        String emailHmac = "";
        
        if(model.mblNo() != null && !model.mblNo().isBlank()) {
        	phoneNorm = Normalizers.normalizePhone(model.mblNo());
        	phoneEnc = (phoneNorm == null) ? null : aesGcmCrypto.encrypt(phoneNorm);
        	phoneHmac = (phoneNorm == null) ? null : hmacSigner.hmacSha256Base64Url(phoneNorm + ":" + model.cmpnyCd());
        }
        
        if(model.email() != null && !model.email().isBlank()) {
        	emailNorm = Normalizers.normalizeEmail(model.email());
        	emailEnc = (emailNorm == null) ? null : aesGcmCrypto.encrypt(emailNorm);
        	emailHmac = (emailNorm == null) ? null : hmacSigner.hmacSha256Base64Url(emailNorm + ":" + model.cmpnyCd());
        }
        
        if(model.birthDt() != null && !model.birthDt().isBlank()) {
        	birthEnc = (birthNorm == null) ? null : aesGcmCrypto.encrypt(birthNorm);
        }

        if (!isMaster && reqSiteCd != null && curSiteCd != null && !reqSiteCd.equals(curSiteCd)) {
            user01Mapper.deleteUserSiteAuth(UserSiteAuthCommand.from(model));
            user01Mapper.insertUserSiteAuth(UserSiteAuthCommand.from(model));
        }

        user01Mapper.mergeUserInfo(UserInfoCommand.from(model, phoneEnc, phoneHmac, emailEnc, emailHmac, birthEnc));
    }
	
	public SiteNodeAdminCandidateListResponse selectSiteNodeAdminCandidateLists(SiteNodeAdminCandidateListParam param) {
		
		SiteNodeAdminCandidateListResponse siteNodeAdminCandidateListResponse = null;
		
		List<UserInfoResult> userInfoList = user01Mapper.selectSiteNodeAdminCandidateLists(SiteNodeAdminCandidateListQuery.from(param));
			
		if(userInfoList != null && userInfoList.size() > 0) {
			siteNodeAdminCandidateListResponse = SiteNodeAdminCandidateListResponse.builder()
																					.userInfoList(userInfoList)
																					.build();
		}
		
		return siteNodeAdminCandidateListResponse;
	}

	@Override
	public void withdrawMyAccount(WithdrawMyAccountParam param) {
		int updated = user01Mapper.withdrawMyAccount(
			new WithdrawMyAccountCommand(param.cmpnyCd(), param.userCd())
		);
		if (updated == 0) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}
	}

	@Override
	public void scheduleWithdrawal(ScheduleWithdrawalParam param) {
		if (param.withdrawalDate() == null || param.withdrawalDate().isBlank()) {
			throw new ApiException(CommonErrorCode.COMMON_400_002);
		}
		int updated = user01Mapper.scheduleWithdrawal(
			new ScheduleWithdrawalCommand(param.cmpnyCd(), param.userCd(), param.withdrawalDate())
		);
		if (updated == 0) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}
	}
}
