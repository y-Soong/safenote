package com.prafta.app.leave.leave01.result;

/**
 * HB-13(F-3) - 시간차(SYS025 02/03/04) CONFIRMED 사용 분을 "사용 / 사용예정"으로 분리한 결과.
 *
 * <p>분리 기준은 웹 {@code Dashboard01Mapper} 의 {@code usedDays}/{@code plannedDays} 와 동일하게
 * {@code START_DATE} 기준이다: 오늘 이하 = 사용(past), 오늘 초과 = 사용예정(planned).
 *
 * <p>배경: 당일분모 전환(E1) 이후 시간차 차감 분모가 날마다 달라져, FE 가 일수→시간을 단일 분모로
 * 역환산하면 실제 3시간이 2시간 48분으로 표시된다(잔결함 F-3). 실제 분 합계를 그대로 내려
 * 역환산 자체를 제거한다. 잔여는 종전대로 역환산 근사치를 유지한다(E4).
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 기준({@code feedback_mybatis_record_column_order}).
 */
public record HourlyUsedSplitRow(
        /** START_DATE <= 오늘 인 시간차 사용 분 합계. */
        int pastMinutes
        /** START_DATE > 오늘 인 시간차 사용예정 분 합계. */
        , int plannedMinutes
) {
}
