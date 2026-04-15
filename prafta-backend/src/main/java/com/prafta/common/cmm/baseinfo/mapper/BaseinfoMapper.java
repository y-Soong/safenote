package com.prafta.common.cmm.baseinfo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.common.cmm.baseinfo.application.command.MblUniqueCheckCommand;
import com.prafta.common.cmm.baseinfo.application.command.SmsAuthNoCommand;
import com.prafta.common.cmm.baseinfo.application.query.AppMenuListQuery;
import com.prafta.common.cmm.baseinfo.application.query.BaseInfoListQuery;
import com.prafta.common.cmm.baseinfo.application.query.BaseInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.CmpnyInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.MblUniqueCheckQuery;
import com.prafta.common.cmm.baseinfo.application.query.MenuListQuery;
import com.prafta.common.cmm.baseinfo.application.query.SiteInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.SiteNodeListQuery;
import com.prafta.common.cmm.baseinfo.application.query.SystInfoListQuery;
import com.prafta.common.cmm.baseinfo.application.query.SystInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.TermsDetailInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.UserIdDupleCheckQuery;
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
	
//	Map<String, Object> selectUserIdInfo(BaseinfoCmmReq dto);
//	
//	void updateUserPw(BaseinfoCmmReq dto);
	
	TermsDetailInfoResult selectTermsDetailInfo(TermsDetailInfoQuery query);
}
