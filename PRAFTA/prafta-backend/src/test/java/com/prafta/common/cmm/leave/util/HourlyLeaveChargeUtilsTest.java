package com.prafta.common.cmm.leave.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시간차 환산 코어 산식 검증 (LC-03 — 설계 문서 §1 수치표·§2 쪼개기 전 케이스 대조).
 *
 * <p>기준: {@code .claude/context/leave-hourly-conversion-design.md} §1(스케줄 4~9h 수치표),
 * §2(쪼개기 누적 판정). conv=480(기본), 마일스톤 3단(반반차 D/4 · 반차 D/2 · 종일 D).
 */
class HourlyLeaveChargeUtilsTest {

    private static final int CONV = 480;

    /** dayTotal 을 plain string 으로 비교(scale 5 고정). */
    private void assertDayTotal(String expected, int cumMinutes, Integer daily) {
        BigDecimal actual = HourlyLeaveChargeUtils.dayTotalDays(cumMinutes, CONV, daily);
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "cum=" + cumMinutes + "분, D=" + daily + " → 기대 " + expected + ", 실제 " + actual);
    }

    @Test
    @DisplayName("설계 §1-① 4h(D=240): 30분=0.0625 / 1h=0.25(반반차 하한) / 2h=0.5 / 종일치=1.0")
    void case4h() {
        assertDayTotal("0.0625", 30, 240);
        assertDayTotal("0.25", 60, 240);   // 반반차 시간(60분) 도달 — 하한 발동(순수 0.125)
        assertDayTotal("0.5", 120, 240);   // 반차 시간(120분) 도달(순수 0.25)
        assertDayTotal("1.0", 240, 240);   // 종일 시간 도달(순수 0.5)
    }

    @Test
    @DisplayName("설계 §1-② 5h(D=300): 1h=0.125(미발동) / 1.5h=0.25 / 2.5h=0.5 / 종일치=1.0")
    void case5h() {
        assertDayTotal("0.125", 60, 300);  // 75분 미만 → 하한 미발동
        assertDayTotal("0.25", 90, 300);   // 75분 도달(순수 0.1875)
        assertDayTotal("0.5", 150, 300);   // 150분 도달(순수 0.3125)
        assertDayTotal("1.0", 300, 300);   // 순수 0.625 → 종일 하한
    }

    @Test
    @DisplayName("설계 §1-③ 6h(D=360): 1h=0.125 / 1.5h=0.25 / 3h=0.5 / 종일치=1.0")
    void case6h() {
        assertDayTotal("0.125", 60, 360);
        assertDayTotal("0.25", 90, 360);   // 90분 도달
        assertDayTotal("0.5", 180, 360);   // 180분 도달(순수 0.375)
        assertDayTotal("1.0", 360, 360);   // 순수 0.75 → 종일 하한
    }

    @Test
    @DisplayName("설계 §1-④ 7h(D=420): 30분=0.0625(순환소수 소멸) / 2h=0.25 / 3.5h=0.5 / 종일치=1.0")
    void case7h() {
        assertDayTotal("0.0625", 30, 420); // 현행 0.07143(1/14 순환) → 0.0625 유한소수
        assertDayTotal("0.125", 60, 420);
        assertDayTotal("0.25", 120, 420);  // 105분 도달 — 하한값과 순수 환산 동일
        assertDayTotal("0.5", 210, 420);   // 210분 도달(순수 0.4375)
        assertDayTotal("1.0", 420, 420);   // 순수 0.875 → 종일 하한
    }

    @Test
    @DisplayName("설계 §1-⑤ 8h(D=480): 전 구간 무변화(하한=순수 환산) — 개편 전후 결과 동일")
    void case8hNoChange() {
        assertDayTotal("0.0625", 30, 480);
        assertDayTotal("0.25", 120, 480);
        assertDayTotal("0.5", 240, 480);
        assertDayTotal("1.0", 480, 480);
    }

    @Test
    @DisplayName("설계 §1-⑥ 9h(D=540): 하한 미발동(순수가 더 비쌈) + 종일치 1.0 캡(1.125 차단)")
    void case9hCap() {
        assertDayTotal("0.125", 60, 540);
        assertDayTotal("0.25", 120, 540);
        assertDayTotal("0.5625", 270, 540); // 반차치 — 비례(0.5)보다 비싸짐(선택의 자유)
        assertDayTotal("1.0", 540, 540);    // 순수 1.125 → 캡(R4)
    }

    @Test
    @DisplayName("설계 §2 쪼개기(D=420): 누적 판정 — 90분(0.1875) + 120분(차액 0.3125) = 반차 하한 0.5")
    void splitCumulative() {
        // 1건차: 90분 — 105분(반반차) 미만 → 하한 미발동
        BigDecimal first = HourlyLeaveChargeUtils.dayTotalDays(90, CONV, 420);
        assertEquals(0, new BigDecimal("0.1875").compareTo(first));
        // 2건차: 누적 210분 = 반차 시간 도달 → 그날 합계 하한 0.5, 이번 건 차액 = 0.5 − 0.1875
        BigDecimal second = HourlyLeaveChargeUtils.dayTotalDays(210, CONV, 420).subtract(first);
        assertEquals(0, new BigDecimal("0.3125").compareTo(second));
    }

    @Test
    @DisplayName("지시서 §1 결함 소멸: 30분×14회(D=420, 종일치) 누적 차감 = 정확히 1.0 (현행 1.00002 오차 소멸)")
    void thirtyMinutesFourteenTimesExactlyOne() {
        BigDecimal charged = BigDecimal.ZERO;
        int cum = 0;
        for (int i = 0; i < 14; i++) {
            cum += 30;
            BigDecimal dayTotal = HourlyLeaveChargeUtils.dayTotalDays(cum, CONV, 420);
            charged = charged.add(dayTotal.subtract(charged)); // 차액 부과 누적 = dayTotal
        }
        assertEquals(0, BigDecimal.ONE.compareTo(charged));
    }

    @Test
    @DisplayName("D=null(스케줄 없음 방어): 하한 미적용 — 순수 환산만")
    void nullDailyNoFloor() {
        assertEquals(0, new BigDecimal("0.25")
                .compareTo(HourlyLeaveChargeUtils.dayTotalDays(120, CONV, null)));
        assertEquals(0, BigDecimal.ZERO
                .compareTo(HourlyLeaveChargeUtils.milestoneFloorDays(120, null)));
    }

    @Test
    @DisplayName("R2 유한소수 conv 검증: 480/600/750/960 허용, 420/360/450/540 거부")
    void terminatingConvValidation() {
        assertTrue(HourlyLeaveChargeUtils.isTerminatingConvMinutes(480));
        assertTrue(HourlyLeaveChargeUtils.isTerminatingConvMinutes(600));
        assertTrue(HourlyLeaveChargeUtils.isTerminatingConvMinutes(750));
        assertTrue(HourlyLeaveChargeUtils.isTerminatingConvMinutes(960));
        assertFalse(HourlyLeaveChargeUtils.isTerminatingConvMinutes(420)); // 1/14 순환
        assertFalse(HourlyLeaveChargeUtils.isTerminatingConvMinutes(360)); // 1/12 순환
        assertFalse(HourlyLeaveChargeUtils.isTerminatingConvMinutes(450)); // 1/15 순환
        assertFalse(HourlyLeaveChargeUtils.isTerminatingConvMinutes(540)); // 1/18 순환
        assertFalse(HourlyLeaveChargeUtils.isTerminatingConvMinutes(0));
    }

    @Test
    @DisplayName("F5 lock 키: 신청(LC-04)·재정산(LC-05) 동일 키 형식")
    void lockKeyFormat() {
        assertEquals("leaveDay:C01:U001:20260711",
                HourlyLeaveChargeUtils.leaveDayLockKey("C01", "U001", "20260711"));
    }
}
