package com.prafta.common.cmm.login.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.login.application.command.ActiveTokenCommand;
import com.prafta.common.cmm.login.application.command.AuthMenuInfoCommand;
import com.prafta.common.cmm.login.application.command.DeviceLoginCommand;
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

	// PRAFTA-app-027-7: 비밀번호 인증 성공 시 실패 카운트/잠금 무조건 초기화.
	void updateUserPwdReset(UserPwdUnlockCommand command);

	void updateUserPwdFail(UserPwdFailCommand command);
	
	int lockUserRow(UserRowLockQuery userRowLockQuery);
	
	void revokeActiveToken(ActiveTokenCommand dto);
	
	void insertAuthToken(ActiveTokenCommand dto);
	
	void updateUserLastLoginDtime(@Param("userCd") String userCd);
	
	int revokeActiveTokenByClient(UserLogoutCommand dto);

    int revokeAllActiveTokens(UserLogoutCommand dto);
	
    String selectUserCd(@Param("cmpnyCd") String cmpnyCd);

	// ===== PRAFTA-046 - 노드-관리자 정합성 가드 (회원가입 경로) =====
	// 노드에 정/부 관리자가 존재하면 1, 아니면(노드 미존재 포함) 0 반환.
	// (web/user mapper 의존을 피해 login 패키지에 동형 쿼리를 둠 — 패키지 경계 보존)
	int selectNodeHasAdmin(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("nodeCd") String nodeCd);

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

	// ===== prafta-com-003 C3 - 디바이스 식별 기반 부정탐지 baseline 적재 (로그인 성공 훅) =====
	/** tb_user_device(현재 상태) upsert(ON DUPLICATE KEY). 재로그인 시 메타/LAST_LOGIN_* 갱신 + DEL_YN='N' 회복. */
	int upsertUserDevice(DeviceLoginCommand command);

	/** tb_user_device_login_hist(append-only) INSERT. PK 는 FNC_CMM_SEQ_NEXTVAL 채번. */
	int insertDeviceLoginHist(DeviceLoginCommand command);
}
