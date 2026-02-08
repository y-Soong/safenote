package com.prafta.app.risk.risk01.dto;

import java.util.List;

import com.prafta.app.chkLst.chkLst01.vo.ChecklistInfo;
import com.prafta.app.risk.risk01.vo.RiskCategory;
import com.prafta.app.risk.risk01.vo.RiskHazard;
import com.prafta.app.risk.risk01.vo.RiskType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class RiskInfoRes {
	List<RiskCategory> riskCategoryList;
	
	List<RiskType> riskTypeList;
	
	List<RiskHazard> riskHazardList;
}
