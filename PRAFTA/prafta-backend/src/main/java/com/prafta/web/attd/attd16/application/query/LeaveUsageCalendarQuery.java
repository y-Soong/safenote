package com.prafta.web.attd.attd16.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd16.application.param.LeaveUsageCalendarParam;

/**
 * ATTD16-T1 - 연차 사용 현황 캘린더 조회 쿼리. 매퍼로 전달되는 조회 조건만 담는다.
 * Attd15 {@code Weekly52hListsQuery} 패턴 동일(토큰 권한/사용자 등 인가용 값은 매퍼로 넘기지 않음).
 */
public record LeaveUsageCalendarQuery(
        String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String monthStart
        , String monthEnd
        , String gvCmpnyCd
) {
    public static LeaveUsageCalendarQuery from(LeaveUsageCalendarParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new LeaveUsageCalendarQuery(
                param.siteCd()
                , param.nodeCd()
                , param.incSubNodeYn()
                , param.monthStart()
                , param.monthEnd()
                , param.gvCmpnyCd()
        );
    }
}
