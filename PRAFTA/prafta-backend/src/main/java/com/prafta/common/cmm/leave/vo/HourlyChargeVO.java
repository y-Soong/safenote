package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

/**
 * 시간차 연차 차감 산출 결과 (LC-03 {@code LeaveDeductionService.calcHourlyCharge}).
 *
 * <p>이번 신청 건에 부과할 차액({@code chargeDays})과, 화면 안내(LC-07 preview)에 쓸
 * 판정 부가정보(하한/캡 발동 여부, 적용 분모)를 함께 싣는다.
 *
 * @param chargeDays      이번 건 부과 차액 = 그날 시간차 차감 합계(dayTotal) − 기존 누적 차감 합
 * @param dayTotalDays    이번 건 반영 후 그날 시간차 차감 합계(하한/캡 반영가)
 * @param convMinutes     적용된 1일 환산시간(분) — 분모(신청 대상일 기준, F4)
 * @param cumMinutesAfter 이번 건 포함 그날 시간차 누적 분
 * @param floorApplied    하한 가드(R3) 발동 여부(마일스톤 요금이 순수 환산을 끌어올림)
 * @param capApplied      상한 캡(R4, 1.0일) 발동 여부
 * @param floorDays       발동한 마일스톤 요금(0.25/0.5/1.0일) — FE 하한 안내 단위 분기용.
 *                        하한 미발동({@code floorApplied=false})이면 {@code null}
 */
public record HourlyChargeVO(
      BigDecimal chargeDays
    , BigDecimal dayTotalDays
    , int convMinutes
    , int cumMinutesAfter
    , boolean floorApplied
    , boolean capApplied
    , BigDecimal floorDays
) {
}
