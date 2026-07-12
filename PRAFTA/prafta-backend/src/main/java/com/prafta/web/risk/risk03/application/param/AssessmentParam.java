package com.prafta.web.risk.risk03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk03.dto.request.AssessmentRequest;

public record AssessmentParam(
	String siteCd
	, String assessmentCd
	, String assessmentStatus
	, String processCd
	
	, String initLikelihoodScore
	, String initSeverityScore
	, String initRiskLv
	// WEB_003 저장 액션: AI 반영 블록이 덧붙은 유해요인설명(INIT_DESC). null 이면 미갱신.
	, String initDesc

	, String revalDate
	, String revalBeforeDesc
	, String revalLikelihoodScore
	, String revalSeverityScore
	, String revalRiskLv
	
	, String revalDesc
	
	, String gvCmpnyCd
	, String gvUserCd
){
	public static AssessmentParam from(AssessmentRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AssessmentParam(
        	request.getSiteCd()
        	, request.getAssessmentCd()
        	, request.getAssessmentStatus()
        	, request.getProcessCd()
        	
        	, request.getInitLikelihoodScore()
        	, request.getInitSeverityScore()
        	, request.getInitRiskLv()
        	, request.getInitDesc()

        	, request.getRevalDate()
        	, request.getRevalBeforeDesc()
        	, request.getRevalLikelihoodScore()
        	, request.getRevalSeverityScore()
        	, request.getRevalRiskLv()
        	
        	, request.getRevalDesc()
        	
        	, tokenInfo.gv_cmpnyCd()
        	, tokenInfo.gv_userCd()
        );
    }
}
