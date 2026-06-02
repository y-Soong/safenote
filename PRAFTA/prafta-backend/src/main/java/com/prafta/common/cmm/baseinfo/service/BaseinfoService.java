package com.prafta.common.cmm.baseinfo.service;

import com.prafta.common.cmm.baseinfo.application.param.AppMenuListParam;
import com.prafta.common.cmm.baseinfo.application.param.BaseInfoListParam;
import com.prafta.common.cmm.baseinfo.application.param.BaseInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.CmpnyInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.MenuListParam;
import com.prafta.common.cmm.baseinfo.application.param.SiteInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.SiteNodeListParam;
import com.prafta.common.cmm.baseinfo.application.param.SystInfoListParam;
import com.prafta.common.cmm.baseinfo.application.param.SystInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.TermsDetailInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.UserIdDupleCheckParam;
import com.prafta.common.cmm.baseinfo.application.param.UserIdInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.UserInfoListParam;
import com.prafta.common.cmm.baseinfo.application.param.UserPasswordParam;
import com.prafta.common.cmm.baseinfo.application.param.UserSmsAuthNoCheckParam;
import com.prafta.common.cmm.baseinfo.application.param.UserSmsAuthNoParam;
import com.prafta.common.cmm.baseinfo.application.param.WebMenuListParam;
import com.prafta.common.cmm.baseinfo.dto.response.AppMenuListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.BaseInfoListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.BaseInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.CmpnyInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.MenuListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.SiteInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.SiteNodeListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.SystInfoListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.SystInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.TermsDetailInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.UserIdDupleCheckResponse;
import com.prafta.common.cmm.baseinfo.dto.response.UserIdInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.UserInfoListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.WebMenuListResponse;

public interface BaseinfoService {
	SystInfoListResponse selectSystinfoList(SystInfoListParam param);
	
	SystInfoResponse selectSystinfo(SystInfoParam param);
	
	BaseInfoListResponse selectBaseinfoList(BaseInfoListParam param);
	
	BaseInfoResponse selectBaseinfo(BaseInfoParam param);
	
	CmpnyInfoResponse selectCmpnyInfo(CmpnyInfoParam param); 
	
	UserIdDupleCheckResponse getUserIdDupleCheck(UserIdDupleCheckParam param);
	
	void insertSmsAuthNo(UserSmsAuthNoParam param);
	
	void userSmsAuthCheck(UserSmsAuthNoCheckParam param);
	
	SiteInfoResponse selectSiteInfoList(SiteInfoParam param);

	SiteNodeListResponse selectSiteNodeList(SiteNodeListParam param);

	// 회원가입(비로그인) 단계 전용 — 동일 SQL 재사용, userCd 권한 조인 없이 회사 내 활성 데이터 노출.
	SiteInfoResponse selectJoinSiteInfoList(com.prafta.common.cmm.baseinfo.application.param.JoinSiteListParam param);

	SiteNodeListResponse selectJoinSiteNodeList(com.prafta.common.cmm.baseinfo.application.param.JoinSiteNodeListParam param);
	
	WebMenuListResponse selectWebMenuList(WebMenuListParam param);
	
	AppMenuListResponse selectAppMenuList(AppMenuListParam param);
	
	MenuListResponse selectMenuList(MenuListParam param);
	
	UserIdInfoResponse selectUserIdInfo(UserIdInfoParam param);

	UserInfoListResponse selectUserInfoList(UserInfoListParam param);

	void updateUserPw(UserPasswordParam param);
	
	TermsDetailInfoResponse selectTermsDetailInfo(TermsDetailInfoParam param);
}
