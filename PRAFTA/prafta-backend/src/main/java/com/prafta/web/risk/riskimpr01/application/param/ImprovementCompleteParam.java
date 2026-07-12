package com.prafta.web.risk.riskimpr01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.riskimpr01.dto.request.ImprovementCompleteRequest;

/**
 * 개선완료(005→003 전이) 파라미터. 식별자는 JWT 에서만 도출.
 */
public record ImprovementCompleteParam(
    String siteCd
    , String processCd
    , String assessmentCd
    , Integer revalLikelihoodScore
    , Integer revalSeverityScore
    , String revalRiskLv
    , String revalDesc
    , String revalDate
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ImprovementCompleteParam from(ImprovementCompleteRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ImprovementCompleteParam(
            request.getSiteCd()
            , request.getProcessCd()
            , request.getAssessmentCd()
            , request.getRevalLikelihoodScore()
            , request.getRevalSeverityScore()
            , request.getRevalRiskLv()
            , request.getRevalDesc()
            , request.getRevalDate()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
