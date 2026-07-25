package com.prafta.web.attd.attd15.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd15.application.param.Weekly52hListsParam;

/**
 * ATTD15-T1 - 주52시간 관리 조회 쿼리. mapper 로 전달되는 조회 조건.
 * Attd11 {@code MonthlyAttdSummaryQuery} 패턴 동일.
 */
public record Weekly52hListsQuery(
        String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String userNm
        , String weekStartYmd
        , String weekEndYmd
        , String gvCmpnyCd
) {
    public static Weekly52hListsQuery from(Weekly52hListsParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new Weekly52hListsQuery(
                param.siteCd()
                , param.nodeCd()
                , param.incSubNodeYn()
                , param.userNm()
                , param.weekStartYmd()
                , param.weekEndYmd()
                , param.gvCmpnyCd()
        );
    }
}
