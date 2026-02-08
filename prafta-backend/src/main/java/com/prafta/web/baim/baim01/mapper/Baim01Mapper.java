package com.prafta.web.baim.baim01.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim01.dto.SiteInfoListQry;
import com.prafta.web.baim.baim01.dto.SiteInfoSave;
import com.prafta.web.baim.baim01.vo.SiteInfo;

@Mapper
public interface Baim01Mapper {
	List<SiteInfo> selectSiteInfoList(@Param(value = "param") SiteInfoListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
//	int updateUserPw(Baim01ReqDto dto);
//	
	void mergeSiteInfo(@Param(value = "param") SiteInfoSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
