package com.prafta.web.baim.baim06.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim06.application.command.CopySiteNodeCommand;
import com.prafta.web.baim.baim06.application.command.SiteNodeCommand;
import com.prafta.web.baim.baim06.application.command.SiteNodeInfoCommand;
import com.prafta.web.baim.baim06.application.query.SiteNodeAdminQuery;
import com.prafta.web.baim.baim06.application.query.SiteNodeCountQuery;
import com.prafta.web.baim.baim06.application.query.SiteNodeListQuery;
import com.prafta.web.baim.baim06.application.query.SiteNodeUserQuery;
import com.prafta.web.baim.baim06.application.query.UserNodeInfoQuery;
import com.prafta.web.baim.baim06.dto.SiteNodeAdminCommand;
import com.prafta.web.baim.baim06.result.SiteNodeResult;
import com.prafta.web.baim.baim06.vo.UserNodeInfo;

@Mapper
public interface Baim06Mapper {

	List<SiteNodeResult> selectSiteNodeList(SiteNodeListQuery query);
	
	void saveSiteNode(SiteNodeInfoCommand command);
	
	int selectNodeCnt(SiteNodeCountQuery query);
	
	void deleteSiteNode(SiteNodeCommand command);
	
	void deleteSiteNodeInUser(SiteNodeCommand command);
	
	void deleteSiteAllNode(SiteNodeCommand command);
	
	void copySiteNode(CopySiteNodeCommand command);
	
	void saveSiteNodeMainAdmin(SiteNodeAdminCommand command);
	
	void deleteSiteNodeMainAdmin(SiteNodeAdminCommand command);
	
	void saveSiteNodeSubAdmin(SiteNodeAdminCommand command);
	
	void deleteSiteNodeSubAdmin(SiteNodeAdminCommand command);
	
	void updateUserNode(SiteNodeAdminCommand command);
	
	int selectSiteNodeInAdmin(SiteNodeAdminQuery query);
	
	int selectSiteNodeInUser(SiteNodeUserQuery query);
	
	UserNodeInfo selectUserNodeInfo(UserNodeInfoQuery query);
	
}
