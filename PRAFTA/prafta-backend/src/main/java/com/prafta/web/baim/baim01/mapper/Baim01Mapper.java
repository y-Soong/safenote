package com.prafta.web.baim.baim01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim01.application.command.MasterSiteAuthSetCommand;
import com.prafta.web.baim.baim01.application.command.SiteAdminSiteAuthCommand;
import com.prafta.web.baim.baim01.application.command.SiteInfoCommand;
import com.prafta.web.baim.baim01.application.command.SiteNodeInfoCommand;
import com.prafta.web.baim.baim01.application.query.SiteInfoListQuery;
import com.prafta.web.baim.baim01.result.SiteInfoResult;

@Mapper
public interface Baim01Mapper {
	List<SiteInfoResult> selectSiteInfoList(SiteInfoListQuery query);
	
	String selectSiteCd(@Param(value = "gvCmpnyCd") String gvCmpnyCd);
	
	void insertSiteNodeInfo(SiteNodeInfoCommand command);
	
	void mergeSiteInfo(SiteInfoCommand command);
	
	void mergeMasterSiteAuthSet(MasterSiteAuthSetCommand command);

	void mergeSiteAdminSiteAuth(SiteAdminSiteAuthCommand command);
}
