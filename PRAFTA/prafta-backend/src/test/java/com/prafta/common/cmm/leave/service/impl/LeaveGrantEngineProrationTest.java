package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * prafta-023 #3 / prafta-029 표준 모델 단위테스트 — AXIS4 반올림(HALF_DAY 0.5 포함) + PRORATE 비례 본연차
 * (순수 로직, Spring/DB 미사용).
 *
 * <p>대상 메서드는 DB·매퍼를 쓰지 않으므로 의존성에 null을 주입해 직접 호출한다(@SpringBootTest 미사용).
 * 반환이 BigDecimal이므로 scale 무시 비교(compareTo)로 단언한다.
 *
 * <p>prafta-029 표준 모델: {@code computeProratedAnnualDays}의 비례 base가
 * "입사가 속한 첫 부분기 일수 = DAYS.between(hire, currentFiscalStart)"로 변경되었다. 따라서
 * {@code currentFiscalStart} 파라미터는 입사 이후 도래한(=crossed된) 회계연도 시작일을 전달한다.
 */
class LeaveGrantEngineProrationTest {

    private final LeaveGrantEngineServiceImpl svc =
            new LeaveGrantEngineServiceImpl(null, null, null, null);

    /** scale 무시 수치 비교 (예: 4 vs 4.0 동일 취급). */
    private static void assertDays(double expected, BigDecimal actual) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }

    @Test
    @DisplayName("AXIS4 반올림: CEIL/ROUND/FLOOR + HALF_DAY 0.5 단위 절사 + 기본(CEIL)")
    void axis4Rounding() {
        // raw 3.78
        assertDays(4, svc.applyAxis4Rounding(3.78, "CEIL"));
        assertDays(3, svc.applyAxis4Rounding(3.78, "FLOOR"));
        assertDays(4, svc.applyAxis4Rounding(3.78, "ROUND"));
        assertDays(3.5, svc.applyAxis4Rounding(3.78, "HALF_DAY")); // floor(7.56)/2 = 3.5

        // HALF_DAY 0.5 경계
        assertDays(7.0, svc.applyAxis4Rounding(7.2, "HALF_DAY")); // floor(14.4)/2 = 7.0
        assertDays(7.5, svc.applyAxis4Rounding(7.6, "HALF_DAY")); // floor(15.2)/2 = 7.5

        // ROUND 경계
        assertDays(8, svc.applyAxis4Rounding(7.5, "ROUND"));
        assertDays(7, svc.applyAxis4Rounding(7.4, "ROUND"));

        // 널/미지정 → CEIL 기본
        assertDays(4, svc.applyAxis4Rounding(3.2, null));
        assertDays(4, svc.applyAxis4Rounding(3.2, "UNKNOWN"));
    }

    @Test
    @DisplayName("PRORATE 비례 본연차(표준모델): 입사 2025-10-01, 도래 회계연도 시작 2026-01-01 → 부분기 92일 → 92/365*15≈3.78")
    void proratedAnnual() {
        // 표준 모델: base = DAYS.between(hire, currentFiscalStart). currentFiscalStart는 입사 이후 도래한 회계연도 시작.
        LocalDate crossedFiscalStart = LocalDate.of(2026, 1, 1);
        assertDays(4, svc.computeProratedAnnualDays(LocalDate.of(2025, 10, 1), crossedFiscalStart, "CEIL"));
        assertDays(3, svc.computeProratedAnnualDays(LocalDate.of(2025, 10, 1), crossedFiscalStart, "FLOOR"));
        assertDays(4, svc.computeProratedAnnualDays(LocalDate.of(2025, 10, 1), crossedFiscalStart, "ROUND"));
        assertDays(3.5, svc.computeProratedAnnualDays(LocalDate.of(2025, 10, 1), crossedFiscalStart, "HALF_DAY"));
    }

    @Test
    @DisplayName("PRORATE 비례 본연차(표준모델): 입사 2025-07-21, 도래 회계연도 시작 2026-01-01 → 부분기 164일 → 164/365*15≈6.74 → CEIL 7")
    void proratedAnnualStandardExample() {
        // prafta-029 스펙 예시: 6.74 → CEIL 7
        LocalDate crossedFiscalStart = LocalDate.of(2026, 1, 1);
        assertDays(7, svc.computeProratedAnnualDays(LocalDate.of(2025, 7, 21), crossedFiscalStart, "CEIL"));
    }

    @Test
    @DisplayName("PRORATE 경계(표준모델): 입사일==도래 회계연도 시작 → 0, 입사~회계연도 시작 1년 초과(비정상) → 1년분 상한 15")
    void proratedEdgeCases() {
        // 입사일이 도래한 회계연도 시작과 같은 날 → 부분기 0일 → 0
        assertDays(0, svc.computeProratedAnnualDays(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), "CEIL"));
        // 입사~회계연도 시작 사이가 1년 초과(비정상) → 365일 상한 → 15
        assertDays(15, svc.computeProratedAnnualDays(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1), "CEIL"));
    }
}
