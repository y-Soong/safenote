package com.prafta.web.risk.risk01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk01.application.model.RiskTypeModel;

public record RiskTypeCommand(
	String cmpnyCd
	, String processCd
	, String riskTypeCd
	, String riskTypeNm
	, String siteCd
	, String useYn
	, String riskTypeDesc
	, String gvCmpnyCd
	, String gvUserCd
){
	public static RiskTypeCommand from(RiskTypeModel model) {

        if (model == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - RiskTypeModel");

        return new RiskTypeCommand(
        	model.cmpnyCd()
        	, model.processCd()
        	, model.riskTypeCd()
        	, model.riskTypeNm()
        	, model.siteCd()
        	, model.useYn()
        	, model.riskTypeDesc()
        	, model.gvCmpnyCd()
        	, model.gvUserCd()
        );
    }
}