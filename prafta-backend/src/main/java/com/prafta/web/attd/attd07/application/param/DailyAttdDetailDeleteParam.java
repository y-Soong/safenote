package com.prafta.web.attd.attd07.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.dto.request.DailyAttdDetailDeleteRequest;

public record DailyAttdDetailDeleteParam(
    String siteCd
    , String userCd
    , String attdId
    , String reason
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static DailyAttdDetailDeleteParam from(DailyAttdDetailDeleteRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - DailyAttdDetailDeleteRequest");

        return new DailyAttdDetailDeleteParam(
            request.getSiteCd()
            , request.getUserCd()
            , request.getAttdId()
            , request.getReason()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
