package com.prafta.web.attd.attd11.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd11.application.param.MonthlyAttdSummaryParam;

/**
 * PRAFTA-034 - Attd_11 월별 사용자 근태 판정 조회 쿼리.
 * mapper 로 전달되는 조회 조건. attd07 MonthlyAttdListQuery 패턴 동일.
 */
public record MonthlyAttdSummaryQuery(
        String workYm
        , String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String userNm
        , String gvCmpnyCd
        , String gvAuthCd
        , String gvUserCd
) {
    public static MonthlyAttdSummaryQuery from(MonthlyAttdSummaryParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new MonthlyAttdSummaryQuery(
                param.workYm()
                , param.siteCd()
                , param.nodeCd()
                , param.incSubNodeYn()
                , param.userNm()
                , param.gvCmpnyCd()
                , param.gvAuthCd()
                , param.gvUserCd()
        );
    }
}
