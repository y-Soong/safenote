package com.prafta.web.user.user01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user01.application.command.ScheduleWithdrawalCommand;
import com.prafta.web.user.user01.application.command.UserInfoCommand;
import com.prafta.web.user.user01.application.command.UserPasswdCommand;
import com.prafta.web.user.user01.application.command.UserSiteAuthCommand;
import com.prafta.web.user.user01.application.command.WithdrawMyAccountCommand;
import com.prafta.web.user.user01.application.query.SiteNodeAdminCandidateListQuery;
import com.prafta.web.user.user01.application.query.UserInfoListQuery;
import com.prafta.web.user.user01.application.query.UserNodeAdminCheckQuery;
import com.prafta.web.user.user01.application.query.UserSiteInfoQuery;
import com.prafta.web.user.user01.result.UserInfoResult;
import com.prafta.web.user.user01.result.UserPwResult;
import com.prafta.web.user.user01.result.UserSiteInfoResult;

@Mapper
public interface User01Mapper {
	int selectUserNodeAdminCheck(UserNodeAdminCheckQuery query);
	
	List<UserInfoResult> selectUserInfoList(UserInfoListQuery query);
	
	int updateUserPw(UserPasswdCommand dto);

	UserPwResult selectUserPwByUserId(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

	int updateMyPw(UserPasswdCommand command);

	int withdrawMyAccount(WithdrawMyAccountCommand command);

	int scheduleWithdrawal(ScheduleWithdrawalCommand command);
	
	UserSiteInfoResult selectUserSiteInfo(UserSiteInfoQuery query);
	
	void mergeUserInfo(UserInfoCommand command);
	
	void deleteUserSiteAuth(UserSiteAuthCommand command);
	
	void insertUserSiteAuth(UserSiteAuthCommand command);
	
	List<UserInfoResult> selectSiteNodeAdminCandidateLists(SiteNodeAdminCandidateListQuery query);
}
