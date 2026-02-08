package com.prafta.web.risk.risk01.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.risk.risk01.dto.RiskHazardListQry;
import com.prafta.web.risk.risk01.dto.RiskHazardQry;
import com.prafta.web.risk.risk01.dto.RiskTypeListQry;
import com.prafta.web.risk.risk01.dto.RiskTypeQry;
import com.prafta.web.risk.risk01.vo.RiskHazard;
import com.prafta.web.risk.risk01.vo.RiskType;

@Mapper
public interface Risk01Mapper {
	
	List<RiskType> selectRiskTypeList(@Param(value = "param") RiskTypeListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void mergeRistType(@Param(value = "param") RiskTypeQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	int selectRiskHazardCnt(@Param(value = "param") RiskTypeQry dto);
	
	void deleteRistType(@Param(value = "param") RiskTypeQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	List<RiskHazard> selectRiskHazardList(@Param(value = "param") RiskHazardListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void mergeRiskHazard(@Param(value = "param") RiskHazardQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void deleteRiskHazard(@Param(value = "param") RiskHazardQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
