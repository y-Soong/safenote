package com.prafta.web.baim.baim06.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim06.dto.CopySiteNodeSave;
import com.prafta.web.baim.baim06.dto.SiteNodeDelete;
import com.prafta.web.baim.baim06.dto.SiteNodeListQry;
import com.prafta.web.baim.baim06.dto.SiteNodeSave;
import com.prafta.web.baim.baim06.vo.SiteNode;

@Mapper
public interface Baim06Mapper {

	List<SiteNode> selectSiteNodeList(@Param(value = "param") SiteNodeListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void saveSiteNode(@Param(value = "param") SiteNodeSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void deleteSiteNode(@Param(value = "param") SiteNodeDelete dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	int selectNodeCnt(@Param(value = "param") SiteNodeDelete dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void deleteSiteAllNode(@Param(value = "param") SiteNodeDelete dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void copySiteNode(@Param(value = "param") CopySiteNodeSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
}
