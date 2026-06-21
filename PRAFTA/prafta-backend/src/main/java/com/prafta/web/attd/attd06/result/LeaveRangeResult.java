package com.prafta.web.attd.attd06.result;

/**
 * prafta-com-013-05-1: 종일 확정 연차 사용 구간(YYYYMMDD) 조회 결과.
 *   휴무일과의 교집합 판정은 서비스단에서 수행한다.
 */
public record LeaveRangeResult(
    String startDate
    , String endDate
) {
}
