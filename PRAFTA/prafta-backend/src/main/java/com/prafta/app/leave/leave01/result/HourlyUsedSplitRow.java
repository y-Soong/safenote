package com.prafta.app.leave.leave01.result;

import java.math.BigDecimal;

/**
 * HB-13(F-3) - 시간차(SYS025 02/03/04) 사용 분 + 반차(SYS025 '01') 사용 일수를
 * "사용 / 사용예정"으로 분리한 결과(CONFIRMED).
 *
 * <p>분리 기준은 웹 {@code Dashboard01Mapper} 의 {@code usedDays}/{@code plannedDays} 와 동일하게
 * {@code START_DATE} 기준이다: 오늘 이하 = 사용(past), 오늘 초과 = 사용예정(planned).
 *
 * <p><b>★ NEW-1(모수 정합)</b>: 집계 모수는 {@code AppLeave01Mapper.selectGroupAgg} 와 동일한
 * <b>활성 부여 집합</b>이다(GRANT_ID 조인 + ACTIVE/EXPIRE_YN='N'/DEL_YN='N' + 미발생 가불 제외,
 * grantTypePrefix 무관 = TOTAL 스코프). 이 값이 {@code groups.TOTAL.used}/{@code planned} 와 같은 셀에
 * 병기되기 때문이며, 모수가 다르면 소비측의 {@code rest = days - 반차일수} 가 음수가 되어 종일분이
 * 표기에서 사라진다. 회계연도 축은 적용하지 않는다(축은 하나여야 한다).
 *
 * <p>배경: 당일분모 전환(E1) 이후 시간차 차감 분모가 날마다 달라져, FE 가 일수→시간을 단일 분모로
 * 역환산하면 실제 3시간이 2시간 48분으로 표시된다(잔결함 F-3). 실제 분 합계를 그대로 내려
 * 역환산 자체를 제거한다. 잔여는 종전대로 역환산 근사치를 유지한다(E4).
 *
 * <p>§20-2(B안): 정수부만 쓰는 표기에서 반차 0.5일이 증발하는 문제 때문에 반차 일수도 함께 내린다.
 * ⚠️ 건수(COUNT)가 아니라 <b>일수 합계</b>다 - 짜투리 분할차감으로 한 반차가 여러 행으로 쪼개질 수
 * 있어 건수 집계는 과다 계상된다. 분할되어도 행 합계는 0.5 로 보존되므로 표기 건수는 소비측이
 * 0.5 로 나누어 산출한다.
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 기준({@code feedback_mybatis_record_column_order}).
 */
public record HourlyUsedSplitRow(
        /** START_DATE 가 오늘 이하인 시간차 사용 분 합계. */
        int pastMinutes
        /** START_DATE 가 오늘 초과인 시간차 사용예정 분 합계. */
        , int plannedMinutes
        /** START_DATE 가 오늘 이하인 반차 사용 일수 합계(건수 아님). */
        , BigDecimal halfDayPastDays
        /** START_DATE 가 오늘 초과인 반차 사용예정 일수 합계(건수 아님). */
        , BigDecimal halfDayPlannedDays
) {
}
