package com.prafta.app.risk.risk01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoQry;
import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoSave;
import com.prafta.app.chkLst.chkLst01.vo.ChecklistInfo;
import com.prafta.app.risk.risk01.dto.RiskAssessmentSave;
import com.prafta.app.risk.risk01.dto.RiskInfoQry;
import com.prafta.app.risk.risk01.vo.RiskCategory;
import com.prafta.app.risk.risk01.vo.RiskHazard;
import com.prafta.app.risk.risk01.vo.RiskType;
import com.prafta.common.dto.TokenInfo;

@Mapper
public interface AppRisk01Mapper {

	List<RiskCategory> selectRiskCategory(@Param(value = "param") RiskInfoQry reqDto, @Param(value = "token") TokenInfo tokenInfo);

	List<RiskType> selectRiskType(@Param(value = "param") RiskInfoQry reqDto, @Param(value = "token") TokenInfo tokenInfo);

	List<RiskHazard> selectRiskHazard(@Param(value = "param") RiskInfoQry reqDto, @Param(value = "token") TokenInfo tokenInfo);

	void mergeRiskAssessment(@Param(value = "param") RiskAssessmentSave reqDto, @Param(value = "token") TokenInfo tokenInfo);




	List<ChecklistInfo> selectChkLstInfo(@Param(value = "param") ChecklistInfoQry reqDto, @Param(value = "token") TokenInfo tokenInfo);

	void mergeChkptInspectAnswer(@Param(value = "param") ChecklistInfoSave reqDto, @Param(value = "token") TokenInfo tokenInfo);
}
