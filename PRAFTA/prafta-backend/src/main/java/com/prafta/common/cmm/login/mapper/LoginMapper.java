package com.prafta.common.cmm.login.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.login.application.command.ActiveTokenCommand;
import com.prafta.common.cmm.login.application.command.AuthMenuInfoCommand;
import com.prafta.common.cmm.login.application.command.RequiredTermsInfoCommand;
import com.prafta.common.cmm.login.application.command.UserJoinCommand;
import com.prafta.common.cmm.login.application.command.UserLogoutCommand;
import com.prafta.common.cmm.login.application.command.UserPwdFailCommand;
import com.prafta.common.cmm.login.application.command.UserPwdUnlockCommand;
import com.prafta.common.cmm.login.application.query.LoginQuery;
import com.prafta.common.cmm.login.application.query.UserRowLockQuery;
import com.prafta.common.cmm.login.application.query.UserTermsAgreementCheckQuery;
import com.prafta.common.cmm.login.result.RequiredTermsResult;
import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.cmm.login.result.UserTermsAgreementCheckResult;

@Mapper
public interface LoginMapper {
	UserResult Login(LoginQuery loginQuery);
	
	void userPwdUnLock(UserPwdUnlockCommand command);
	
	void updateUserPwdFail(UserPwdFailCommand command);
	
	int lockUserRow(UserRowLockQuery userRowLockQuery);
	
	void revokeActiveToken(ActiveTokenCommand dto);
	
	void insertAuthToken(ActiveTokenCommand dto);
	
	void updateUserLastLoginDtime(@Param("userCd") String userCd);
	
	int revokeActiveTokenByClient(UserLogoutCommand dto);

    int revokeAllActiveTokens(UserLogoutCommand dto);
	
    String selectUserCd(@Param("cmpnyCd") String cmpnyCd);
    
	int insertUserInfo(UserJoinCommand dto);
	
	int insertUserSiteAuth(UserJoinCommand dto);
	
	List<RequiredTermsResult> selectRequiredTermsList();
	
	int insertTermsUserAgrMgmt(RequiredTermsInfoCommand dto);
	
	List<UserTermsAgreementCheckResult> selectUserTermsAgreementCheck(UserTermsAgreementCheckQuery userTermsAgreementCheckQuery);

	void mergeAuthMenuInfo(AuthMenuInfoCommand command);

	// ===== PRAFTA-036 - 휴대폰 인증 후 활성화 흐름 =====
	UserResult selectUserByUserCd(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

	int selectUserMblHmacConflict(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd, @Param("mblNoHmac") String mblNoHmac);

	int activateAccount(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

	int updateUserMblNo(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd, @Param("mblNoEnc") String mblNoEnc, @Param("mblNoHmac") String mblNoHmac, @Param("mblNoLast4") String mblNoLast4);
}
