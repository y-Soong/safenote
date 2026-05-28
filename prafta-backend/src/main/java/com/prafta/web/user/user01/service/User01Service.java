package com.prafta.web.user.user01.service;

import com.prafta.web.user.user01.application.model.UserInfoModel;
import com.prafta.web.user.user01.application.param.HireDateHistoryParam;
import com.prafta.web.user.user01.application.param.HireDateImpactParam;
import com.prafta.web.user.user01.application.param.LeaveInfoParam;
import com.prafta.web.user.user01.application.param.MyPasswdParam;
import com.prafta.web.user.user01.application.param.MyProfileParam;
import com.prafta.web.user.user01.application.param.ScheduleWithdrawalParam;
import com.prafta.web.user.user01.application.param.SiteNodeAdminCandidateListParam;
import com.prafta.web.user.user01.application.param.UserCreditParam;
import com.prafta.web.user.user01.application.param.UserHireDateParam;
import com.prafta.web.user.user01.application.param.UserInfoListParam;
import com.prafta.web.user.user01.application.param.UserPasswdParam;
import com.prafta.web.user.user01.application.param.WithdrawMyAccountParam;
import com.prafta.web.user.user01.application.param.WithdrawalCancelParam;
import com.prafta.web.user.user01.dto.response.HireDateHistoryResponse;
import com.prafta.web.user.user01.dto.response.HireDateImpactResponse;
import com.prafta.web.user.user01.dto.response.LeaveInfoResponse;
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

	// ===== PRAFTA-017-4 - 근태/연차 정보 (master/hr 전용) =====
	LeaveInfoResponse selectLeaveInfo(LeaveInfoParam param);

	void updateUserCredit(UserCreditParam param);

	HireDateImpactResponse analyzeHireDateImpact(HireDateImpactParam param);

	void updateUserHireDate(UserHireDateParam param);

	HireDateHistoryResponse selectHireDateHistory(HireDateHistoryParam param);
}
