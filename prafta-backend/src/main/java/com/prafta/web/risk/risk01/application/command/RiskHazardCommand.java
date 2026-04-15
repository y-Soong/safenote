package com.prafta.web.risk.risk01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk01.application.model.RiskHazardModel;

public record RiskHazardCommand(
	String cmpnyCd
	, String riskTypeCd
	, String hazardCd
	, String hazardNm
	, String siteCd
	, String hazardDesc
	, String gvCmpnyCd
	, String gvUserCd
) {
    public static RiskHazardCommand from(RiskHazardModel model) {

        if(model == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - RiskHazardModel");

        return new RiskHazardCommand(
        	model.cmpnyCd()
        	, model.riskTypeCd()
        	, model.hazardCd()
        	, model.hazardNm()
        	, model.siteCd()
        	, model.hazardDesc()
        	, model.gvCmpnyCd()
        	, model.gvUserCd()
        );
    }
}