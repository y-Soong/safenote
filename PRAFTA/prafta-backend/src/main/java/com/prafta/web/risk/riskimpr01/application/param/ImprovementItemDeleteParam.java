package com.prafta.web.risk.riskimpr01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.riskimpr01.dto.request.ImprovementItemDeleteRequest;

/**
 * 개선항목 삭제 파라미터. 식별자는 JWT 에서만 도출.
 */
public record ImprovementItemDeleteParam(
    String siteCd
    , String processCd
    , String assessmentCd
    , Integer improvementSeq
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ImprovementItemDeleteParam from(ImprovementItemDeleteRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ImprovementItemDeleteParam(
            request.getSiteCd()
            , request.getProcessCd()
            , request.getAssessmentCd()
            , request.getImprovementSeq()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
