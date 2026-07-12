package com.prafta.common.cmm.baseinfo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.common.cmm.baseinfo.application.command.MblUniqueCheckCommand;
import com.prafta.common.cmm.baseinfo.application.command.SmsAuthConsumeCommand;
import com.prafta.common.cmm.baseinfo.application.command.SmsAuthNoCommand;
import com.prafta.common.cmm.baseinfo.application.command.UserPasswordCommand;
import com.prafta.common.cmm.baseinfo.application.query.AppMenuListQuery;
import com.prafta.common.cmm.baseinfo.application.query.BaseInfoListQuery;
import com.prafta.common.cmm.baseinfo.application.query.BaseInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.CmpnyInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.MblUniqueCheckQuery;
import com.prafta.common.cmm.baseinfo.application.query.MenuListQuery;
import com.prafta.common.cmm.baseinfo.application.query.SiteInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.SmsVerifiedCheckQuery;
import com.prafta.common.cmm.baseinfo.application.query.SiteNodeListQuery;
import com.prafta.common.cmm.baseinfo.application.query.SystInfoListQuery;
import com.prafta.common.cmm.baseinfo.application.query.SystInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.TermsDetailInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.UserIdDupleCheckQuery;
import com.prafta.common.cmm.baseinfo.application.query.UserIdInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.UserInfoListQuery;
import com.prafta.common.cmm.baseinfo.application.query.UserSmsAuthNoCheckQuery;
import com.prafta.common.cmm.baseinfo.application.query.WebMenuListQuery;
import com.prafta.common.cmm.baseinfo.result.AppMenuResult;
import com.prafta.common.cmm.baseinfo.result.BaseInfoResult;
import com.prafta.common.cmm.baseinfo.result.CmpnyInfoResult;
import com.prafta.common.cmm.baseinfo.result.MenuInfoResult;
import com.prafta.common.cmm.baseinfo.result.SiteInfoResult;
import com.prafta.common.cmm.baseinfo.result.SiteNodeInfoResult;
import com.prafta.common.cmm.baseinfo.result.SystInfoResult;
import com.prafta.common.cmm.baseinfo.result.TermsDetailInfoResult;
import com.prafta.common.cmm.baseinfo.result.UserIdInfoResult;
import com.prafta.common.cmm.baseinfo.result.UserInfoResult;
import com.prafta.common.cmm.baseinfo.result.WebMenuResult;

@Mapper
public interface BaseinfoMapper {
	List<SystInfoResult> selectSystinfoList(SystInfoListQuery reqDto);
	
	List<SystInfoResult> selectSystinfo(SystInfoQuery dto);
	
	List<BaseInfoResult> selectBaseinfoList(BaseInfoListQuery query);
	
	List<BaseInfoResult> selectBaseinfo(BaseInfoQuery query);
	
	CmpnyInfoResult selectCmpnyInfo(CmpnyInfoQuery query);
	
	String getUserIdDupleCheck(UserIdDupleCheckQuery query);
	
	int selectMblUniqChk(MblUniqueCheckQuery dto);
	
	void insertSmsAuthNo(SmsAuthNoCommand dto);
	
	int updateSmsAuthReq(MblUniqueCheckCommand mblUniqueCheckCommand);
	
	String selectCertNoSmsId(UserSmsAuthNoCheckQuery dto);
	
	List<SiteInfoResult> selectSiteInfoList(SiteInfoQuery query);
	
	List<SiteNodeInfoResult> selectSiteNodeList(SiteNodeListQuery query);
	
	List<WebMenuResult> selectWebMenuList(WebMenuListQuery query);
	
	List<AppMenuResult> selectAppMenuList(AppMenuListQuery query);
	
	List<MenuInfoResult> selectMenuList(MenuListQuery query);

	/**
	 * 메뉴 조회 시 isFavorite 세팅용 — 사용자별 즐겨찾기 MENU_D_ID 목록.
	 * USER_CD/CMPNY_CD 는 JWT 도출값만 전달(IDOR 방지).
	 */
	List<String> selectMyFavoriteMenuDIds(
			@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd
			, @org.apache.ibatis.annotations.Param("userCd") String userCd);

	List<UserInfoResult> selectUserInfoList(UserInfoListQuery query);

	UserIdInfoResult selectUserIdInfo(UserIdInfoQuery query);
	
	void updateUserPw(UserPasswordCommand command);

	/* prafta-app-032 D: 비밀번호 재설정 시 일용직 로그인 테이블(TB_DAILY_USER.USER_PW) 동기 갱신. 정규 사용자엔 0행 no-op. */
	int updateDailyUserPw(UserPasswordCommand command);

	/* 비밀번호 재설정 진입 시 대상 사용자의 최근 SMS 인증 성공(미만료/미소비) 레코드 SMS_ID 조회 */
	String selectSmsVerifiedSmsId(SmsVerifiedCheckQuery query);

	/* 비밀번호 재설정 검증에 사용된 SMS 인증 레코드를 소비(consume) 처리 */
	int consumeSmsAuth(SmsAuthConsumeCommand command);
	
	TermsDetailInfoResult selectTermsDetailInfo(TermsDetailInfoQuery query);
}
