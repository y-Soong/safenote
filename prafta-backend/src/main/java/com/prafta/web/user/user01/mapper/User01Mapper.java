package com.prafta.web.user.user01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user01.application.command.ScheduleWithdrawalCommand;
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
	
	int updateUserPw(UserPasswdCommand dto);

	UserPwResult selectUserPwByUserId(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

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
}
