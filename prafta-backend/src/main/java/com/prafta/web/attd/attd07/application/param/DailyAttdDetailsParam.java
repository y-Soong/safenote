package com.prafta.web.attd.attd07.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.dto.request.DailyAttdDetailsRequest;

public record DailyAttdDetailsParam(
	String attdId
    , String siteCd
    , String userCd
    , String userId
    , String workYmd
    , String nodeCd
    , String gvCmpnyCd
) {
    public static DailyAttdDetailsParam from(DailyAttdDetailsRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - DailyAttdDetailsRequest");

        return new DailyAttdDetailsParam(
        	request.getAttdId()
            , request.getSiteCd()
            , request.getUserCd()
            , request.getUserId()
            , request.getWorkYmd()
            , request.getNodeCd()
            , tokenInfo.gv_cmpnyCd()
        );
    }
}
