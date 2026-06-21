package com.prafta.common.cmm.login.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.login.application.command.ActiveTokenCommand;
import com.prafta.common.cmm.login.application.command.AuthMenuInfoCommand;
import com.prafta.common.cmm.login.application.command.DeviceLoginCommand;
import com.prafta.common.cmm.login.application.command.DeviceOccupancyAnomalyCommand;
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

	/**
	 * (계정당 활성 디바이스 1대) 같은 USER_CD 의 다른 기기를 비활성화한다(DEL_YN='Y').
	 * 현재 로그인 기기(deviceUuid)는 제외 → "마지막 로그인 기기"만 푸시 수신 대상으로 남는다.
	 * 그 기기가 다시 로그인하면 upsertUserDevice 가 DEL_YN='N' 으로 되살린다(항상 최신=활성).
	 */
	int deactivateOtherUserDevices(@Param("userCd") String userCd, @Param("deviceUuid") String deviceUuid);

	// ===== prafta-com-015 015-1 - 디바이스 점유 재할당 이상탐지 + 감사 =====
	/**
	 * 해당 DEVICE_UUID 의 현재 점유자(USER_CD)를 반환한다(DEL_YN 무관, 행 없으면 null).
	 * 로그인 upsert 직전에 호출해 "점유자 변경" 여부를 판정한다.
	 */
	String selectDeviceOwner(@Param("deviceUuid") String deviceUuid);

	/** 디바이스 점유 재할당 이상행 적재(tb_user_device_occupancy_anomaly). ANOMALY_NO 는 채번. */
	int insertOccupancyAnomaly(DeviceOccupancyAnomalyCommand command);

	// ===== prafta-com-015 015-3 - 로그아웃 시 현재 기기 푸시 토큰 정리(stale 누수 차단) =====
	/**
	 * 해당 사용자의 해당 기기(DEVICE_UUID + USER_CD)의 PUSH_TOKEN 을 NULL 로 정리한다.
	 * IDOR 안전(WHERE 에 USER_CD 동반). 로그아웃 best-effort 호출.
	 */
	int clearPushTokenForDevice(@Param("userCd") String userCd, @Param("deviceUuid") String deviceUuid);
}
