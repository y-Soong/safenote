package com.prafta.web.subcon.subcon03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 승인 사전정보(마감 상태 + 미마감 월 + 릴레이 후보) 조회 파라미터(PRAFTA-SUBCON-T3 §5-4·§5-7).
 */
public record ShareReqApproveInfoParam(
    Long shareReqId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ShareReqApproveInfoParam from(Long shareReqId, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ShareReqApproveInfoParam(
            shareReqId
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
