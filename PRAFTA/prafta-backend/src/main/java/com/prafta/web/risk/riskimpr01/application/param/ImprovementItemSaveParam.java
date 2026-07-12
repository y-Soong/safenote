package com.prafta.web.risk.riskimpr01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.riskimpr01.dto.request.ImprovementItemSaveRequest;

/**
 * 개선항목 upsert 파라미터. 식별자는 JWT 에서만 도출.
 */
public record ImprovementItemSaveParam(
    String siteCd
    , String processCd
    , String assessmentCd
    , Integer improvementSeq
    , String improveDate
    , String improveDesc
    , Integer likelihoodScore
    , Integer severityScore
    , String riskLv
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ImprovementItemSaveParam from(ImprovementItemSaveRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ImprovementItemSaveParam(
            request.getSiteCd()
            , request.getProcessCd()
            , request.getAssessmentCd()
            , request.getImprovementSeq()
            , request.getImproveDate()
            , request.getImproveDesc()
            , request.getLikelihoodScore()
            , request.getSeverityScore()
            , request.getRiskLv()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
