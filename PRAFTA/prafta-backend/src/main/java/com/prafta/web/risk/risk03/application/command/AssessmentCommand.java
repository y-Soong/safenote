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
	// WEB_003 저장 액션: AI 반영 블록이 덧붙은 유해요인설명(INIT_DESC). null 이면 미갱신(동적 SET).
	, String initDesc

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
        // PRAFTA_COM_001_T6 Low-1A: 위험도(INIT/REVAL_RISK_LV)는 서버에서 빈도 x 강도로 재계산한 값을 사용한다(클라 전송값 불신뢰).
        return from(param, fileMgmtCd, param != null ? param.initRiskLv() : null, param != null ? param.revalRiskLv() : null);
    }

	public static AssessmentCommand from(AssessmentParam param, String fileMgmtCd, String initRiskLv, String revalRiskLv) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (fileMgmtCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AssessmentCommand(
        	param.siteCd()
        	, param.assessmentCd()
        	, param.assessmentStatus()
        	, param.processCd()

        	, param.initLikelihoodScore()
        	, param.initSeverityScore()
        	, initRiskLv
        	, param.initDesc()

        	, param.revalDate()
        	, param.revalBeforeDesc()
        	, param.revalLikelihoodScore()
        	, param.revalSeverityScore()
        	, revalRiskLv

        	, param.revalDesc()
        	, fileMgmtCd

        	, param.gvCmpnyCd()
        	, param.gvUserCd()
        );
    }
}
