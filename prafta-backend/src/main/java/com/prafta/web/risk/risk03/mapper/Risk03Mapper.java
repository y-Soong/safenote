package com.prafta.web.risk.risk03.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.risk.risk03.dto.RiskAssessmentsListQry;
import com.prafta.web.risk.risk03.dto.SaveAssessmentQry;
import com.prafta.web.risk.risk03.vo.RiskAssessment;
import com.prafta.web.risk.risk03.vo.RiskType;

@Mapper
public interface Risk03Mapper {
	
	List<RiskType> selectRiskTypeList(@Param(value = "token") Map<String, Object> tokenInfo);
	
	List<RiskAssessment> selectRiskAssessmentsLists(@Param(value = "param") RiskAssessmentsListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void updateAssessment(@Param(value = "param") SaveAssessmentQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
