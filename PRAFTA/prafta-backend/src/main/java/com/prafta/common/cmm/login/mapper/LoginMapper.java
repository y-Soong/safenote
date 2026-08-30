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
import com.prafta.common.cmm.login.result.JoinConflictResult;
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

	/**
	 * 비밀번호 잠금 잔여분 조회 — DB 서버 시계 기준(세션 타임존 무관).
	 *
	 * <p>PWD_LOCK_EXPIRE_DTIME 은 DATE_ADD(NOW(), ...) 로 DB 서버 시계로 기록되므로,
	 * Java 시계(LocalDateTime.now())와 비교하면 세션 타임존이 UTC 인 운영에서 항상 "만료"로
	 * 오판되어 잠금이 무력화된다. 잔여 판정을 DB 로 옮겨 서버 시계끼리만 비교한다.
	 *
	 * @return 잔여 분(올림). 잠금 아님/만료면 null
	 */
	Integer selectPwdLockRemainMinutes(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

	int lockUserRow(UserRowLockQuery userRowLockQuery);
	
	void revokeActiveToken(ActiveTokenCommand dto);
	
	void insertAuthToken(ActiveTokenCommand dto);
	
	/**
	 * 최종 로그인 시각 갱신.
	 *
	 * <p>★테넌트 격리: USER_CD 는 회사별 채번이라 전역 유일하지 않다. CMPNY_CD 를 동반하지 않으면
	 *   로그인할 때마다 다른 회사의 동일 USER_CD 사용자 행까지 갱신된다.
	 */
	void updateUserLastLoginDtime(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);
	
	int revokeActiveTokenByClient(UserLogoutCommand dto);

    int revokeAllActiveTokens(UserLogoutCommand dto);
	
    String selectUserCd(@Param("cmpnyCd") String cmpnyCd);

	// ===== PRAFTA-046 - 노드-관리자 정합성 가드 (회원가입 경로) =====
	// 노드에 정/부 관리자가 존재하면 1, 아니면(노드 미존재 포함) 0 반환.
	// (web/user mapper 의존을 피해 login 패키지에 동형 쿼리를 둠 — 패키지 경계 보존)
	int selectNodeHasAdmin(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("nodeCd") String nodeCd);

	/**
	 * 로그인 ID 사용중 여부(전사 기준 — 일용직 포함).
	 *
	 * <p>★로그인 ID 전역 유일화: 로그인은 회사코드 없이 USER_ID 만으로 사용자를 찾으므로 ID 는 전역 유일해야 한다
	 *   (UNIQUE UX_TB_USER_ID(USER_ID)). 셀프 회원가입에 서버측 사전검사가 없으면 INSERT 에서 제약 위반 500 이 난다.
	 *
	 * <p>★소정-04 이후 셀프가입 경로는 본 메서드를 쓰지 않는다. 거부('07') 행 재활용 재가입 때문에
	 *   "TB_USER 점유 = 무조건 차단"이 성립하지 않게 되어, {@link #selectJoinConflictByUserId}(상태까지 판정)
	 *   + {@link #selectDailyUserIdExists}(일용직) 조합으로 분리했다. 본 메서드는 동일 판정이 필요한
	 *   다른 경로를 위해 남겨 둔다.
	 */
	int selectUserIdExistsGlobal(@Param("userId") String userId);

	// ===== PRAFTA-SUBCON-T2-07 - 연동 미러 사업장 가입 차단 (회원가입 경로) =====
	// 대상 사업장이 미러(LINK_SRC_CMPNY_CD NOT NULL)면 1 이상 → 가입 거부.
	// (subcon mapper 의존을 피해 login 패키지에 동형 술어를 둠 — 모듈 경계 보존, plan §5-7)
	int selectMirrorSiteCnt(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

	int insertUserInfo(UserJoinCommand dto);

	int insertUserSiteAuth(UserJoinCommand dto);

	// ===== 소정-04 - 셀프가입 승인제 ('06 가입승인대기') + 거부('07') 행 재활용 재가입 =====

	/**
	 * 로그인 ID 사용중 여부 — <b>일용직(TB_DAILY_USER)만</b> 판정한다.
	 *
	 * <p>재가입 재활용 경로에서는 TB_USER 쪽 점유가 "재활용 대상인 본인의 '07' 행"일 수 있어
	 * {@link #selectUserIdExistsGlobal}(TB_USER + 일용직 합산)을 그대로 쓰면 자기 자신 때문에
	 * 막힌다. TB_USER 점유는 {@link #selectJoinConflictByUserId} 로 상태까지 보고 판정하고,
	 * 일용직 ID 충돌만 본 메서드로 별도 확인한다(전역 유일 규칙은 그대로 유지).
	 */
	int selectDailyUserIdExists(@Param("userId") String userId);

	/**
	 * 해당 로그인 ID 를 점유 중인 TB_USER 행 1건 (전역 — USER_ID 는 전역 유일).
	 *
	 * @return 점유 행(회사/사용자코드/상태/사용여부). 미점유면 null
	 */
	JoinConflictResult selectJoinConflictByUserId(@Param("userId") String userId);

	/**
	 * 해당 휴대폰 HMAC 을 점유 중인 TB_USER 행 1건 (회사 스코프 — UNIQUE 가 CMPNY_CD+MBL_NO_HMAC).
	 *
	 * @return 점유 행. 미점유면 null
	 */
	JoinConflictResult selectJoinConflictByMblHmac(@Param("cmpnyCd") String cmpnyCd,
			@Param("mblNoHmac") String mblNoHmac);

	/**
	 * 거부('07') 행 재활용 재가입 — 신청 정보 전량 갱신 + ACCOUNT_STATUS '07' → '06' 전이.
	 *
	 * <p>UNIQUE 제약(USER_ID 전역 / CMPNY_CD+MBL_NO_HMAC) 때문에 신규 INSERT 로는 같은 아이디·
	 * 휴대폰 재가입이 불가능하므로 기존 행을 되살린다(plan §8 Q2 확정). WHERE 에
	 * {@code ACCOUNT_STATUS='07'} 를 두어 활성 계정이 이 경로로 덮어써지는 것을 원천 차단한다.
	 *
	 * @return 갱신 행 수 (0 = 대상이 '07' 이 아님 → 동시성/변조)
	 */
	int updateRejectedUserForRejoin(UserJoinCommand dto);

	/**
	 * 재가입 시 <b>이전 신청 사업장</b>의 사업장 권한을 회수한다 (USE_YN='N').
	 *
	 * <p>거부 행을 재활용하면 이전 신청 때 만들어진 TB_USER_SITE_AUTH 행이 그대로 남는다.
	 * 다른 사업장으로 재신청한 뒤 승인되면 그 계정이 <b>예전 사업장 데이터까지</b> 접근하게 되므로
	 * (SiteAccessService 는 이 원장을 본다) 새 신청 사업장 외의 권한을 내린다.
	 *
	 * @param siteCd 이번 신청 사업장(유지 대상)
	 * @return 회수된 행 수
	 */
	int deactivateOtherUserSiteAuth(@Param("cmpnyCd") String cmpnyCd,
			@Param("userCd") String userCd, @Param("siteCd") String siteCd);

	/**
	 * 승인대기('06')·거부('07') 계정 1건을 로그인 ID 로 조회한다 (로그인 안내 분기 전용).
	 *
	 * <p>두 상태 모두 USE_YN='N' 이라 {@link #Login} 쿼리에 잡히지 않는다(security M-2 —
	 * 미승인자가 재직자 목록에 유입되지 않게 하기 위한 적재 규약). 안내 메시지는
	 * <b>비밀번호 검증을 통과한 경우에만</b> 노출한다(계정 존재 탐색 방지) — 그 검증에 쓸
	 * USER_PW 를 포함해 반환한다.
	 */
	UserResult selectSelfJoinInactiveUserByUserId(@Param("userId") String userId);

	// ===== [security H-1] 셀프가입 휴대폰 본인인증 서버 강제 =====

	/**
	 * 해당 휴대폰(HMAC)의 <b>최근 인증 완료</b> SMS 기록 1건 (PURPOSE_CD='SELF_JOIN', 30분 창).
	 *
	 * <p>비밀번호 재설정 흐름({@code BaseinfoMapper.selectSmsVerifiedSmsId})의 미러이며,
	 * 가입 시점에는 TB_USER 행이 없으므로 사용자 조인 대신 휴대폰 HMAC 으로 찾는다.
	 *
	 * @param certNo 선택 대조용 인증번호(없으면 코드 대조 생략)
	 * @return SMS_ID. 인증 기록이 없거나 창을 벗어났으면 null(→ 가입 차단)
	 */
	String selectSelfJoinVerifiedSmsId(@Param("mblNoHmac") String mblNoHmac,
			@Param("certNo") String certNo);

	/**
	 * 인증 기록 소비('Y' → 'C'). 한 번의 인증이 여러 계정 생성에 재사용되지 않게 한다.
	 *
	 * @return 소비된 행 수(1 이 아니면 동시 요청이 먼저 소비 → 호출부에서 차단)
	 */
	int consumeSelfJoinSmsAuth(@Param("smsId") String smsId);
	
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
	 * (계정당 활성 디바이스 1대) 같은 회사·같은 USER_CD 의 다른 기기를 비활성화한다(DEL_YN='Y').
	 * 현재 로그인 기기(deviceUuid)는 제외 → "마지막 로그인 기기"만 푸시 수신 대상으로 남는다.
	 * 그 기기가 다시 로그인하면 upsertUserDevice 가 DEL_YN='N' 으로 되살린다(항상 최신=활성).
	 *
	 * <p>★테넌트 격리: USER_CD 는 회사별 채번이라 전역 유일하지 않다. CMPNY_CD 없이 실행하면
	 *   다른 회사의 동일 USER_CD 사용자의 기기까지 비활성화(강제 로그아웃)한다.
	 */
	int deactivateOtherUserDevices(@Param("cmpnyCd") String cmpnyCd,
			@Param("userCd") String userCd, @Param("deviceUuid") String deviceUuid);

	// ===== prafta-com-015 015-1 - 디바이스 점유 재할당 이상탐지 + 감사 =====
	/**
	 * 해당 DEVICE_UUID 의 현재 점유자(USER_CD)를 반환한다(DEL_YN 무관, 행 없으면 null).
	 * 로그인 upsert 직전에 호출해 "점유자 변경" 여부를 판정한다.
	 *
	 * <p>회사 조건을 걸지 않는 것은 의도다 — "같은 기기를 다른 회사 사용자가 점유"하는 경우도
	 *   탐지 대상이기 때문이다. 다만 USER_CD 가 회사별 채번이라, 다른 회사의 동일 USER_CD 를
	 *   같은 사용자로 오판할 수 있다(탐지 누락). 이상탐지는 부가 기능(실패해도 로그인 무영향)이라
	 *   현 단계에서는 한계를 명시만 하고, 정밀 판정은 후속 과제로 둔다.
	 */
	String selectDeviceOwner(@Param("deviceUuid") String deviceUuid);

	/** 디바이스 점유 재할당 이상행 적재(tb_user_device_occupancy_anomaly). ANOMALY_NO 는 채번. */
	int insertOccupancyAnomaly(DeviceOccupancyAnomalyCommand command);

	// ===== prafta-com-015 015-3 - 로그아웃 시 현재 기기 푸시 토큰 정리(stale 누수 차단) =====
	/**
	 * 해당 사용자의 해당 기기(CMPNY_CD + DEVICE_UUID + USER_CD)의 PUSH_TOKEN 을 NULL 로 정리한다.
	 * IDOR 안전(WHERE 에 회사·USER_CD 동반). 로그아웃 best-effort 호출.
	 */
	int clearPushTokenForDevice(@Param("cmpnyCd") String cmpnyCd,
			@Param("userCd") String userCd, @Param("deviceUuid") String deviceUuid);
}
