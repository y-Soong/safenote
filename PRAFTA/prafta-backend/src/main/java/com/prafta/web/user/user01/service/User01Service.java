package com.prafta.web.user.user01.service;

import com.prafta.web.user.user01.application.model.UserInfoModel;
import com.prafta.web.user.user01.application.param.HireDateHistoryParam;
import com.prafta.web.user.user01.application.param.HireDateImpactParam;
import com.prafta.web.user.user01.application.param.LeaveInfoParam;
import com.prafta.web.user.user01.application.param.MyPasswdParam;
import com.prafta.web.user.user01.application.param.MyProfileParam;
import com.prafta.web.user.user01.application.param.ScheduleWithdrawalParam;
import com.prafta.web.user.user01.application.param.SiteNodeAdminCandidateListParam;
import com.prafta.web.user.user01.application.param.UserCreateParam;
import com.prafta.web.user.user01.application.param.UserCreditParam;
import com.prafta.web.user.user01.application.param.UserHireDateParam;
import com.prafta.web.user.user01.application.param.UserInfoListParam;
import com.prafta.web.user.user01.application.param.UpdateMyDefaultSchParam;
import com.prafta.web.user.user01.application.param.UserPasswdParam;
import com.prafta.web.user.user01.application.param.WithdrawMyAccountParam;
import com.prafta.web.user.user01.application.param.WithdrawalCancelParam;
import com.prafta.web.user.user01.dto.response.HireDateHistoryResponse;
import com.prafta.web.user.user01.dto.response.HireDateImpactResponse;
import com.prafta.web.user.user01.dto.response.LeaveInfoResponse;
import com.prafta.web.user.user01.dto.response.MyProfileResponse;
import com.prafta.web.user.user01.dto.response.SiteNodeAdminCandidateListResponse;
import com.prafta.web.user.user01.dto.response.UserInfoListResponse;

import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.dto.TokenInfo;

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

	// ===== PRAFTA-036 - 관리자 단건 사용자 생성 =====
	void insertUserOne(UserCreateParam param);

	// ===== PRAFTA-036 - 엑셀 양식 다운로드 (PRAFTA-037-F5 감사 컨텍스트 추가) =====
	byte[] buildUserCreateTemplate(TokenInfo tokenInfo, AuditContext auditContext);

	// ===== PRAFTA-COM-008-E-5 - 기본 근무타입 select 옵션(대상 사업장 활성 근무타입) =====
	java.util.List<com.prafta.common.cmm.sch.vo.SchOptionVO> getSchTypeOptions(String cmpnyCd, String siteCd);

	// ===== F-8-2 - 본인 기본 근무타입 자기변경(웹 내정보, 세션 사업장 고정) =====
	java.util.List<com.prafta.common.cmm.sch.vo.SchOptionVO> getMyDefaultSchOptions(String cmpnyCd, String userCd);

	void updateMyDefaultSch(UpdateMyDefaultSchParam param);
}
