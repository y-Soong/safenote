package com.prafta.web.attd.attd07.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailsParam;

public record DailyAttdDetailsQuery(
	String attdId
    , String siteCd
    , String userCd
    , String userId
    , String workYmd
    , String nodeCd
    , String gvCmpnyCd
) {
    public static DailyAttdDetailsQuery from(DailyAttdDetailsParam param) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - DailyAttdDetailsParam");

        return new DailyAttdDetailsQuery(
        	param.attdId()
            , param.siteCd()
            , param.userCd()
            , param.userId()
            , param.workYmd()
            , param.nodeCd()
            , param.gvCmpnyCd()
        );
    }
}
