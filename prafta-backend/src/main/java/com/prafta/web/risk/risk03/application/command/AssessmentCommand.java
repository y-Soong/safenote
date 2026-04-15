package com.prafta.web.risk.risk03.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk03.application.param.AssessmentParam;

public record AssessmentCommand(
	String siteCd
	, String assessmentCd
	, String assessmentStatus
	, String processCd
	
	, String initLikelihoodScore
	, String initSeverityScore
	, String initRiskLv
	
	, String revalDate
	, String revalBeforeDesc
	, String revalLikelihoodScore
	, String revalSeverityScore
	, String revalRiskLv
	
	, String revalDesc
	, String revalFileMgmtCd
	
	, String gvCmpnyCd
	, String gvUserCd
){
	public static AssessmentCommand from(AssessmentParam param, String fileMgmtCd) {

        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - AssessmentParam");
        if (fileMgmtCd == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - fileMgmtCd");

        return new AssessmentCommand(
        	param.siteCd()
        	, param.assessmentCd()
        	, param.assessmentStatus()
        	, param.processCd()
        	
        	, param.initLikelihoodScore()
        	, param.initSeverityScore()
        	, param.initRiskLv()
        	
        	, param.revalDate()
        	, param.revalBeforeDesc()
        	, param.revalLikelihoodScore()
        	, param.revalSeverityScore()
        	, param.revalRiskLv()
        	
        	, param.revalDesc()
        	, fileMgmtCd
        	
        	, param.gvCmpnyCd()
        	, param.gvUserCd()
        );
    }
}
