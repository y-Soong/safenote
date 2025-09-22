package com.prafta.web.chkLst.chkLst01.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim02.dto.Baim02;
import com.prafta.web.chkLst.chkLst01.dto.ChkLst01;
import com.prafta.web.chkLst.chkLst01.dto.ChkLst01ReqDto;

@Mapper
public interface ChkLst01Mapper {
	List<ChkLst01> selectChkptList(@Param(value = "param") ChkLst01ReqDto dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void mergeChkptList(@Param(value = "param") ChkLst01 dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void updateChkptList(@Param(value = "param") ChkLst01 dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
