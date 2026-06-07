package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.LinkSnapshotRequest;

public record LinkSnapshotParam(
    String siteCd
    , String acctId
    , String linkDomainCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static LinkSnapshotParam from(LinkSnapshotRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new LinkSnapshotParam(
            request.getSiteCd()
            , request.getAcctId()
            , request.getLinkDomainCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
