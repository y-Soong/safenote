package com.prafta.app.nearmiss.nearmiss01.application.param;

import com.prafta.app.nearmiss.nearmiss01.dto.request.MyReportListRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * A2 내 보고 목록 Param. 본인(REPORTER_ID=gvUserCd) 보고건만 조회한다.
 */
public record MyReportListParam(
    String reportStatusCd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static MyReportListParam from(MyReportListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new MyReportListParam(
            request.getReportStatusCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
