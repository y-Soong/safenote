package com.prafta.web.user.user01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user01.application.command.DefaultSchChangeReqInsertCommand;
import com.prafta.web.user.user01.application.command.ScheduleWithdrawalCommand;
import com.prafta.web.user.user01.application.command.UserCreateInsertCommand;
import com.prafta.web.user.user01.application.command.UserCreditDeleteCommand;
import com.prafta.web.user.user01.application.command.UserCreditInsertCommand;
import com.prafta.web.user.user01.application.command.UserHireDateHistoryCommand;
import com.prafta.web.user.user01.application.command.UserHireDateUpdateCommand;
import com.prafta.web.user.user01.application.command.UserInfoCommand;
import com.prafta.web.user.user01.application.command.UserPasswdCommand;
import com.prafta.web.user.user01.application.command.UserSiteAuthCommand;
import com.prafta.web.user.user01.application.command.WithdrawMyAccountCommand;
import com.prafta.web.user.user01.application.command.WithdrawalCancelCommand;
import com.prafta.web.user.user01.application.query.LeaveInfoQuery;
import com.prafta.web.user.user01.application.query.MyProfileQuery;
import com.prafta.web.user.user01.application.query.SiteNodeAdminCandidateListQuery;
import com.prafta.web.user.user01.application.query.UserInfoListQuery;
import com.prafta.web.user.user01.application.query.UserNodeAdminCheckQuery;
import com.prafta.web.user.user01.application.query.UserSiteInfoQuery;
import com.prafta.web.user.user01.result.MyProfileResult;
import com.prafta.web.user.user01.result.ServiceCreditResult;
import com.prafta.web.user.user01.result.TemplateAuthRow;
import com.prafta.web.user.user01.result.TemplateNodeRow;
import com.prafta.web.user.user01.result.TemplateRankRow;
import com.prafta.web.user.user01.result.TemplateSiteRow;
import com.prafta.web.user.user01.result.UserHireDateHistoryResult;
import com.prafta.web.user.user01.result.UserHireInfoResult;
import com.prafta.web.user.user01.result.UserInfoResult;
import com.prafta.web.user.user01.result.UserPwResult;
import com.prafta.web.user.user01.result.UserSiteInfoResult;
import com.prafta.web.user.user01.result.UserStatutoryLeaveSummaryResult;

@Mapper
public interface User01Mapper {
	int selectUserNodeAdminCheck(UserNodeAdminCheckQuery query);

	List<UserInfoResult> selectUserInfoList(UserInfoListQuery query);

	// PRAFTA-049 — 사용자 생성 양식 참조 시트(사업장/소속부서/권한) 조회. 회사 스코프 고정.
	List<TemplateSiteRow> selectTemplateSiteList(@Param("cmpnyCd") String cmpnyCd);

	List<TemplateNodeRow> selectTemplateNodeList(@Param("cmpnyCd") String cmpnyCd);

	List<TemplateAuthRow> selectTemplateAuthList(@Param("cmpnyCd") String cmpnyCd);

	List<TemplateRankRow> selectTemplateRankList(@Param("cmpnyCd") String cmpnyCd);

	int updateUserPw(UserPasswdCommand dto);

	UserPwResult selectUserPwByUserId(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

	// 관리자 비밀번호 초기화용 — 대상 회원의 암호화된 휴대폰번호(MBL_NO_ENC) 조회.
	String selectUserMblNoEnc(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

	int updateMyPw(UserPasswdCommand command);

	int withdrawMyAccount(WithdrawMyAccountCommand command);

	int scheduleWithdrawal(ScheduleWithdrawalCommand command);
	
	int cancelWithdrawal(WithdrawalCancelCommand command);
	
	UserSiteInfoResult selectUserSiteInfo(UserSiteInfoQuery query);
	
	void mergeUserInfo(UserInfoCommand command);
	
	void deleteUserSiteAuth(UserSiteAuthCommand command);
	
	void insertUserSiteAuth(UserSiteAuthCommand command);
	
	List<UserInfoResult> selectSiteNodeAdminCandidateLists(SiteNodeAdminCandidateListQuery query);

	MyProfileResult selectMyProfile(MyProfileQuery query);

	// ===== PRAFTA-017-4 - 근태/연차 정보 (입사일 + 경력 인정) =====
	UserHireInfoResult selectUserHireInfo(LeaveInfoQuery query);

	List<ServiceCreditResult> selectUserServiceCreditList(LeaveInfoQuery query);

	// 대상 userCd가 자사(cmpnyCd)에 실제 존재하는 사용자인지 확인 (고아 경력 인정 레코드 방지)
	int selectUserExistCount(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("userCd") String userCd);

	int deleteUserServiceCredit(UserCreditDeleteCommand command);

	int insertUserServiceCredit(UserCreditInsertCommand command);

	String selectUserHireDate(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("userCd") String userCd);

	int updateUserHireDate(UserHireDateUpdateCommand command);

	// prafta-032: 입사일 변경 이력 HIST_ID 채번 (연차 조정 멱등키 _HD{histId}에 사용하기 위해 INSERT 전 미리 채번)
	String selectNextHireHistId(@Param("cmpnyCd") String cmpnyCd);

	int insertUserHireDateHistory(UserHireDateHistoryCommand command);

	// 입사일 변경 이력 목록 조회 (변경자명 조인, INSERT_DATE 내림차순)
	List<UserHireDateHistoryResult> selectUserHireDateHistory(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

	// 활성 법정 연차 부여 합계 조회 (prafta-022 작업 F, 입사일 변경 영향분석 실측)
	UserStatutoryLeaveSummaryResult selectUserStatutoryLeaveSummary(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("userCd") String userCd);

	// ===== PRAFTA-036 - 관리자 단건/엑셀 사용자 생성 =====
	// USER_CD 채번 (LoginMapper.selectUserCd 와 동일 SQL — 모듈 결합 회피 위해 복제)
	String selectNextUserCd(@Param("cmpnyCd") String cmpnyCd);

	// SITE_NO -> SITE_CD 매핑 (없으면 null)
	String selectSiteCdBySiteNo(@Param("cmpnyCd") String cmpnyCd, @Param("siteNo") String siteNo);

	// 사이트 노드 존재 검증 (CMPNY_CD + SITE_CD + NODE_CD 매칭 행 1건 이상이면 1 반환)
	int selectSiteNodeExists(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("nodeCd") String nodeCd);

	// ===== PRAFTA-046 - 노드-관리자 정합성 가드 =====
	// 노드에 정(MAIN) 또는 부(SUB) 관리자가 존재하면 1, 아니면(노드 미존재 포함) 0 반환.
	int selectNodeHasAdmin(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("nodeCd") String nodeCd);

	// 회사 내 USER_ID 중복 여부 (있으면 1)
	int selectUserIdExists(@Param("cmpnyCd") String cmpnyCd, @Param("userId") String userId);

	// 회사 내 휴대폰 HMAC 중복 여부 (있으면 1)
	int selectUserMblHmacExists(@Param("cmpnyCd") String cmpnyCd, @Param("mblNoHmac") String mblNoHmac);

	// AUTH_CD 권한레벨 조회 (sortIdx — 없으면 null)
	String selectAuthLevelByAuthCd(@Param("cmpnyCd") String cmpnyCd, @Param("authCd") String authCd);

	// 신규 사용자 INSERT (ACCOUNT_STATUS='04' 인증대기 고정)
	int insertOneUser(UserCreateInsertCommand command);

	// 단건용 TB_USER_SITE_AUTH INSERT (insertUserSiteAuth(UPSERT) 와 별개로 신규 행만 추가)
	int insertOneUserSiteAuth(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd, @Param("siteCd") String siteCd, @Param("gvUserCd") String gvUserCd);

	// ===== PRAFTA-037-F7 - 단건 생성 시 추가 사이트 권한 부여 =====
	// SITE_CD 존재 검증 (회사 내 SITE_CD 매칭 행 1건 이상이면 1 반환)
	int selectSiteExistsBySiteCd(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

	// ===== PRAFTA-042-5 - 전사 접근 역할(master/hr/safe) 사업장권한 자동부여/회수 =====
	// 변경 전 현재 AUTH_CD 조회 (역할 변경 판정용, 없으면 null)
	String selectUserAuthCd(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

	// 전 사업장(tb_site) 권한을 사용자에게 멱등 부여 (ON DUP UPDATE USE_YN='Y')
	int insertAllSiteAuthForUser(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd, @Param("gvUserCd") String gvUserCd);

	// 자동부여 사업장권한 회수 — 소속 사업장(homeSiteCd) 1건만 잔존, 나머지 USE_YN='N' (D7)
	int deleteSiteAuthExceptHome(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd, @Param("homeSiteCd") String homeSiteCd, @Param("gvUserCd") String gvUserCd);

	// ===== PRAFTA-001(기본근무타입-승인제, 2026-08-27) - 웹 본인 기본 근무타입 변경 요청등록(REQ_TYPE='14') =====

	/** REQ_ID 채번(AppMypage01Mapper.selectNextDefaultSchReqId 와 동일 시퀀스 'ATTD_REQ_ID' — 단일 PK 공간 공유). */
	String selectNextDefaultSchReqId(@Param("cmpnyCd") String cmpnyCd);

	/** F15 advisory lock 획득(AppMypage01Mapper.getAdvisoryLock 미러 — MD5 래핑, 락 이름 64자 제한 대응). */
	Integer getAdvisoryLock(@Param("lockKey") String lockKey, @Param("timeoutSec") int timeoutSec);

	/** F15 advisory lock 해제(AppMypage01Mapper.releaseAdvisoryLock 미러). */
	Integer releaseAdvisoryLock(@Param("lockKey") String lockKey);

	/**
	 * 동일 사용자의 대기중(REQ_STATUS='01') 기본 근무타입 변경 요청 개수(P10 중복 차단).
	 * WORK_YMD 조건 없음(이 요청 유형은 특정 근무일에 종속되지 않음).
	 */
	int countPendingDefaultSchChangeReq(@Param("cmpnyCd") String cmpnyCd,
	                                    @Param("siteCd") String siteCd,
	                                    @Param("userCd") String userCd);

	/** tb_user_attd_req INSERT(REQ_TYPE='14' 전용). */
	int insertDefaultSchChangeReq(DefaultSchChangeReqInsertCommand cmd);
}
