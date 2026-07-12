package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.AttdTbmPrintRequest;

/**
 * 근태 + TBM 합본 출력(③) 파라미터.
 * 식별자(cmpnyCd/userCd/authCd)는 JWT 클레임에서만 도출한다(body 신뢰 금지).
 */
public record AttdTbmPrintParam(
    String siteCd
    , String acctId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static AttdTbmPrintParam from(AttdTbmPrintRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AttdTbmPrintParam(
            request.getSiteCd()
            , request.getAcctId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
