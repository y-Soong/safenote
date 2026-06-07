package com.prafta.web.acct.acct01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.LinkConfirmRequest;

public record LinkConfirmParam(
    String siteCd
    , String acctId
    , String linkDomainCd
    , List<LinkConfirmRequest.LinkItem> items
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static LinkConfirmParam from(LinkConfirmRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new LinkConfirmParam(
            request.getSiteCd()
            , request.getAcctId()
            , request.getLinkDomainCd()
            , request.getItems()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
