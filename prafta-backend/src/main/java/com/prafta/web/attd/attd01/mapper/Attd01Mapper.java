package com.prafta.web.attd.attd01.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd01.dto.SchCdQry;
import com.prafta.web.attd.attd01.dto.SchInfoHistQry;
import com.prafta.web.attd.attd01.dto.SchInfoHistSave;
import com.prafta.web.attd.attd01.dto.SchInfoListQry;
import com.prafta.web.attd.attd01.dto.SchInfoSave;
import com.prafta.web.attd.attd01.vo.SchHist;
import com.prafta.web.attd.attd01.vo.SchInfo;

@Mapper
public interface Attd01Mapper {
	
	List<SchInfo> selectSchInfoList(@Param(value = "param") SchInfoListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	String selectSchCd(@Param(value = "param") SchCdQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void updateSchInfo(@Param(value = "param") SchInfoSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	int selectSchHistIdx(@Param(value = "param") SchInfoHistQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void insertSchHistInfo(@Param(value = "param") SchInfoHistSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	List<SchHist> selectSchHistList(@Param(value = "param") SchInfoHistQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
