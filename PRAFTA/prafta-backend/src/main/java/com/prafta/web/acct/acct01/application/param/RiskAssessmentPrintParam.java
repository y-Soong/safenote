package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.RiskAssessmentPrintRequest;

/**
 * 위험성평가 개선실행계획서/개선완료보고서 출력(②) 보강 파라미터.
 * 식별자(cmpnyCd/userCd/authCd)는 JWT 클레임에서만 도출한다(body 신뢰 금지).
 */
public record RiskAssessmentPrintParam(
    String siteCd
    , String acctId
    , String assessmentCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static RiskAssessmentPrintParam from(RiskAssessmentPrintRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new RiskAssessmentPrintParam(
            request.getSiteCd()
            , request.getAcctId()
            , request.getAssessmentCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
