package com.prafta.web.baim.baim01.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim01.dto.Baim01;
import com.prafta.web.baim.baim01.dto.Baim01ReqDto;

@Mapper
public interface Baim01Mapper {
	List<Map<String, Object>> selectSiteInfoList(Baim01ReqDto dto);
	
//	int updateUserPw(Baim01ReqDto dto);
//	
	void mergeSiteInfo(@Param(value = "param") Baim01 dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
