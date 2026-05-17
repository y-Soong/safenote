package com.prafta.web.user.user01.service;

import com.prafta.web.user.user01.application.model.UserInfoModel;
import com.prafta.web.user.user01.application.param.MyPasswdParam;
import com.prafta.web.user.user01.application.param.MyProfileParam;
import com.prafta.web.user.user01.application.param.ScheduleWithdrawalParam;
import com.prafta.web.user.user01.application.param.SiteNodeAdminCandidateListParam;
import com.prafta.web.user.user01.application.param.UserInfoListParam;
import com.prafta.web.user.user01.application.param.UserPasswdParam;
import com.prafta.web.user.user01.application.param.WithdrawMyAccountParam;
import com.prafta.web.user.user01.application.param.WithdrawalCancelParam;
import com.prafta.web.user.user01.dto.response.MyProfileResponse;
import com.prafta.web.user.user01.dto.response.SiteNodeAdminCandidateListResponse;
import com.prafta.web.user.user01.dto.response.UserInfoListResponse;

public interface User01Service {
	UserInfoListResponse selectUserInfoList(UserInfoListParam param);

	int updateUserPw(UserPasswdParam param);

	void updateMyPw(MyPasswdParam param);

	void withdrawMyAccount(WithdrawMyAccountParam param);

	void scheduleWithdrawal(ScheduleWithdrawalParam param);

	void cancelWithdrawal(WithdrawalCancelParam param);
	
	void updateOneUserInfo(UserInfoModel model);
	
	SiteNodeAdminCandidateListResponse selectSiteNodeAdminCandidateLists(SiteNodeAdminCandidateListParam param);

	MyProfileResponse selectMyProfile(MyProfileParam param);
}
