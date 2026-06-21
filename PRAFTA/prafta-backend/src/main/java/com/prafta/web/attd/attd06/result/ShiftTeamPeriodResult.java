package com.prafta.web.attd.attd06.result;

/**
 * prafta-com-013-05-2: 교대팀 현재 적용기간(STR_DATE/END_DATE) 조회 결과.
 *   기간 연장 판정의 기준값으로 사용한다.
 */
public record ShiftTeamPeriodResult(
    String strDate
    , String endDate
) {
}
