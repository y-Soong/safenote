package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.VictimSearchRequest;

public record VictimSearchParam(
    String siteCd
    , String userNm
    , String userTypeCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static VictimSearchParam from(VictimSearchRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new VictimSearchParam(
            request.getSiteCd()
            , request.getUserNm()
            , request.getUserTypeCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
