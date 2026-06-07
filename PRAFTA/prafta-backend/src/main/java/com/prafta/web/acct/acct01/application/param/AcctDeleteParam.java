package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.AcctDeleteRequest;

public record AcctDeleteParam(
    String siteCd
    , String acctId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static AcctDeleteParam from(AcctDeleteRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AcctDeleteParam(
            request.getSiteCd()
            , request.getAcctId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
