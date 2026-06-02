package com.prafta.app.nearmiss.nearmiss01.application.query;

import com.prafta.app.nearmiss.nearmiss01.application.param.MyReportListParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * A2 내 보고 목록 조회 Query (본인 보고건만).
 */
public record MyReportListQuery(
    String reportStatusCd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static MyReportListQuery from(MyReportListParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new MyReportListQuery(
            param.reportStatusCd()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
