package com.prafta.web.attd.attd12.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd12.application.param.FraudAttdSuspectParam;

/**
 * prafta-com-003 C6 - 부정 출퇴근 의심 조회 쿼리(mapper 전달 조건).
 * Attd_11 MonthlyAttdSummaryQuery 패턴 동일.
 */
public record FraudAttdSuspectQuery(
        String workYm
        , String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String gvCmpnyCd
) {
    public static FraudAttdSuspectQuery from(FraudAttdSuspectParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new FraudAttdSuspectQuery(
                param.workYm()
                , param.siteCd()
                , param.nodeCd()
                , param.incSubNodeYn()
                , param.gvCmpnyCd()
        );
    }
}
