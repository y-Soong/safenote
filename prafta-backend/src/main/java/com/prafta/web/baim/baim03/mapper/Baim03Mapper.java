package com.prafta.web.baim.baim03.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim03.dto.Baim03;
import com.prafta.web.baim.baim03.dto.Baim03ReqDto;

@Mapper
public interface Baim03Mapper {
	List<Baim03> selectTermsList(@Param(value = "param") Baim03ReqDto dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	List<Baim03> selectTermsDList(@Param(value = "param") Baim03ReqDto dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	Baim03 selectTermsInfo(@Param(value = "param") Baim03ReqDto dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void mergeTermsInfo(@Param(value = "param") Baim03 dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void insertTermsIdVersionInfo(@Param(value = "param") Baim03 dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void deleteCmmCodeDetailInfo(@Param(value = "param") Baim03 dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
