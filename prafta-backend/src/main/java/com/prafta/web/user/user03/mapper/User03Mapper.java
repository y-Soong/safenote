package com.prafta.web.user.user03.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user03.dto.SiteInfoListQry;
import com.prafta.web.user.user03.dto.User03;
import com.prafta.web.user.user03.vo.SiteInfo;

@Mapper
public interface User03Mapper {
	List<SiteInfo> selectSiteInfoSearch(@Param(value = "param") SiteInfoListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void mergeUserSiteAuth(@Param(value = "param") User03 dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
