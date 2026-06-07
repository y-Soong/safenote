package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.AcctListRequest;

public record AcctListParam(
    String siteCd
    , String acctGradeCd
    , String processStatusCd
    , String startDate
    , String endDate
    , String searchKeyword
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static AcctListParam from(AcctListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AcctListParam(
            request.getSiteCd()
            , request.getAcctGradeCd()
            , request.getProcessStatusCd()
            , request.getStartDate()
            , request.getEndDate()
            , request.getSearchKeyword()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
