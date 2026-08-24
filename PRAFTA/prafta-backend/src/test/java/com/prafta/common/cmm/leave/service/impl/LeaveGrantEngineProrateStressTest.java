package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.prafta.common.cmm.leave.vo.LeavePolicyVO;

/**
 * 작업지시서_PRORATE-봉인해제_스트레스테스트.md §3 (S-1~S-10) — 엔진 단위 검증.
 *
 * <p>{@code resolveFiscalEntitlement}는 today를 명시 파라미터로 받아 결정적이므로(LocalDate.now() 미사용)
 * 임의 시나리오 날짜를 직접 주입해 검증한다. 반환은 {@link LeaveGrantEngineServiceImpl#resolveFiscalEntitlementForTest}
 * 로 평탄화된 grantType→일수 합계 맵(STATUTORY_ANNUAL/STATUTORY_TENURE_BONUS).
 *
 * <p>스코프: 이 클래스는 순수 엔진 계산만 검증한다(Spring/DB 미사용, LeaveGrantEngineProrationTest와 동일 패턴).
 * 다음 항목은 이 테스트로 커버되지 않는다(작업지시서 §3 원문 산출물에 실기동/화면 검증으로 별도 기록):
 * <ul>
 *   <li>S-7 차액 조회 탭 연동(computeHireBasisAccrual + Attd_09 화면, DB 필요)</li>
 *   <li>S-9 Baim_07 화면 저장/재조회 왕복(웹 UI 필요)</li>
 *   <li>S-10 실제 DB IDEMPOTENCY_KEY 유니크 제약 위반 여부(이 테스트는 "같은 입력→같은 출력"인
 *       계산 결정성만 확인 — 실제 중복 INSERT 방지는 08-23 LeaveGrantScheduler 실측(16건 전부 유니크)으로
 *       이미 별도 확인됨)</li>
 *   <li>S-8 HIRE_DATE/NEXT_YEAR_BULK 무회귀는 코드 경로 분리로 구조적 보장(resolveEntitlement가 AXIS1=FISCAL_YEAR일
 *       때만 resolveFiscalEntitlement를 호출하고, 이 작업은 그 메서드 내부를 전혀 수정하지 않았음 — 별도 런타임
 *       테스트 불요, 코드 인용: LeaveGrantEngineServiceImpl.java:1587-1588)</li>
 * </ul>
 */
class LeaveGrantEngineProrateStressTest {

    private final LeaveGrantEngineServiceImpl svc =
            new LeaveGrantEngineServiceImpl(null, null, null, null);

    private static LeavePolicyVO fiscalPolicy(String axis3, String axis4) {
        LeavePolicyVO p = new LeavePolicyVO();
        p.setAxis1GrantBase("FISCAL_YEAR");
        p.setAxis2FiscalStartMm("01");
        p.setAxis2FiscalStartDd("01");
        p.setAxis3FirstYearMethod(axis3);
        p.setAxis4ProrateRounding(axis4);
        p.setAxis5TenureMode("LEGAL");
        return p;
    }

    private static void assertDays(double expected, Map<String, BigDecimal> totals, String grantType) {
        BigDecimal actual = totals.getOrDefault(grantType, BigDecimal.ZERO);
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual + " (totals=" + totals + ")");
    }

    @Test
    @DisplayName("S-1: 2025-07-12 입사, FISCAL+PRORATE+CEIL → 첫 회계기준일(2026-01-01) 비례 8일(173/365*15=7.11→CEIL)")
    void s1_standardCase() {
        LeavePolicyVO policy = fiscalPolicy("PRORATE", "CEIL");
        LocalDate hire = LocalDate.of(2025, 7, 12);
        LocalDate today = LocalDate.of(2026, 1, 1); // 입사 후 첫 회계연도 시작(crossed==1)

        Map<String, BigDecimal> totals = svc.resolveFiscalEntitlementForTest(policy, hire, today, 5, 0);

        assertDays(8, totals, "STATUTORY_ANNUAL");
        assertFalse(totals.containsKey("STATUTORY_TENURE_BONUS"), "첫 회계연도엔 근속가산 없음");
    }

    @Test
    @DisplayName("S-2: 2025-12-31 입사 + CEIL → 다음날(2026-01-01) 비례 1일(1/365*15=0.041→CEIL)")
    void s2_yearEndHire_ceil() {
        LeavePolicyVO policy = fiscalPolicy("PRORATE", "CEIL");
        LocalDate hire = LocalDate.of(2025, 12, 31);
        LocalDate today = LocalDate.of(2026, 1, 1);

        Map<String, BigDecimal> totals = svc.resolveFiscalEntitlementForTest(policy, hire, today, 0, 0);

        assertDays(1, totals, "STATUTORY_ANNUAL");
    }

    @Test
    @DisplayName("S-3: 2025-12-31 입사 + FLOOR → 0일이고, 0일이면 부여 행 자체가 생성되지 않음(엔진 signum>0 가드)")
    void s3_yearEndHire_floor_noRowWhenZero() {
        LeavePolicyVO policy = fiscalPolicy("PRORATE", "FLOOR");
        LocalDate hire = LocalDate.of(2025, 12, 31);
        LocalDate today = LocalDate.of(2026, 1, 1);

        Map<String, BigDecimal> totals = svc.resolveFiscalEntitlementForTest(policy, hire, today, 0, 0);

        // floor(0.041) = 0 → resolveFiscalEntitlement의 "prorated.signum() > 0" 가드에 걸려 컴포넌트 자체가 안 생긴다.
        assertFalse(totals.containsKey("STATUTORY_ANNUAL"), "0일이면 STATUTORY_ANNUAL 행이 생성되지 않아야 함");
        assertTrue(totals.isEmpty(), "본연차 외 다른 컴포넌트도 없어야 함(월차는 이 메서드 스코프 밖)");
    }

    @Test
    @DisplayName("S-4: 2025-12-31 입사 + HALF_DAY → 0.5일 단위 절사도 0(floor(0.041*2)/2=0), 부여 행 없음")
    void s4_yearEndHire_halfDay() {
        LeavePolicyVO policy = fiscalPolicy("PRORATE", "HALF_DAY");
        LocalDate hire = LocalDate.of(2025, 12, 31);
        LocalDate today = LocalDate.of(2026, 1, 1);

        Map<String, BigDecimal> totals = svc.resolveFiscalEntitlementForTest(policy, hire, today, 0, 0);

        // floor(0.041*2)/2 = floor(0.082)/2 = 0/2 = 0 → S-3과 동일하게 행 자체가 없음.
        assertFalse(totals.containsKey("STATUTORY_ANNUAL"), "HALF_DAY도 이 극단값에서는 0일이라 행이 없어야 함");
    }

    @Test
    @DisplayName("S-5a: 1/1 입사, 입사 당일(crossed==1) — PRORATE는 부분기간 0이라 행 없음, NEXT_YEAR_BULK는 즉시 15(정책 특성상 서로 다름)")
    void s5a_jan1Hire_onHireDay_policiesDiverge() {
        LocalDate hire = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2026, 1, 1); // 입사=회계연도 시작=오늘, crossed==1 즉시 도래

        Map<String, BigDecimal> prorate =
                svc.resolveFiscalEntitlementForTest(fiscalPolicy("PRORATE", "CEIL"), hire, today, 0, 0);
        Map<String, BigDecimal> bulk =
                svc.resolveFiscalEntitlementForTest(fiscalPolicy("NEXT_YEAR_BULK", "CEIL"), hire, today, 0, 0);

        // division/edge 무오류(예외 없이 계산됨)가 핵심 확인 대상.
        assertFalse(prorate.containsKey("STATUTORY_ANNUAL"), "PRORATE: 부분기간=0(DAYS.between(hire,hire)=0)이라 행 없음");
        assertDays(15, bulk, "STATUTORY_ANNUAL"); // NEXT_YEAR_BULK는 부분기간 계산 자체를 안 하고 즉시 15 일괄
    }

    @Test
    @DisplayName("S-5b: 1/1 입사, 다음 회계기준일(2027-01-01, crossed==2) — PRORATE/NEXT_YEAR_BULK 둘 다 crossed>=2 분기로 수렴해 15로 동일(타임라인 §6.1)")
    void s5b_jan1Hire_secondFiscalStart_policiesConverge() {
        LocalDate hire = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2027, 1, 1); // crossed==2 (2026-01-01, 2027-01-01 둘 다 hire~today 범위)

        Map<String, BigDecimal> prorate =
                svc.resolveFiscalEntitlementForTest(fiscalPolicy("PRORATE", "CEIL"), hire, today, 12, 0);
        Map<String, BigDecimal> bulk =
                svc.resolveFiscalEntitlementForTest(fiscalPolicy("NEXT_YEAR_BULK", "CEIL"), hire, today, 12, 0);

        // crossed>=2 분기는 AXIS3를 아예 보지 않으므로 두 정책이 완전히 동일한 코드 경로를 탄다.
        assertDays(15, prorate, "STATUTORY_ANNUAL");
        assertDays(15, bulk, "STATUTORY_ANNUAL");
        assertFalse(prorate.containsKey("STATUTORY_TENURE_BONUS"), "근속 1년차(tenureYear=crossed-1=1)라 가산 없음(LEGAL 3년차부터)");
    }

    @Test
    @DisplayName("S-6: 윤년(2028-02-29) 입사 — 재직일수 계산 무오류(division/edge), 결과는 0~15일 범위")
    void s6_leapYearHire_noException() {
        LeavePolicyVO policy = fiscalPolicy("PRORATE", "CEIL");
        LocalDate hire = LocalDate.of(2028, 2, 29); // 유효한 윤년 2/29
        LocalDate today = LocalDate.of(2029, 1, 1); // 입사 후 첫 회계연도 시작(crossed==1)

        Map<String, BigDecimal> totals = assertDoesNotThrow(
                () -> svc.resolveFiscalEntitlementForTest(policy, hire, today, 10, 0),
                "윤년 입사일 계산에서 예외가 발생하면 안 됨");

        BigDecimal annual = totals.getOrDefault("STATUTORY_ANNUAL", BigDecimal.ZERO);
        assertTrue(annual.compareTo(BigDecimal.ZERO) >= 0 && annual.compareTo(BigDecimal.valueOf(15)) <= 0,
                () -> "비례 본연차는 0~15일 범위여야 함, 실제=" + annual);
        // 참고: DAYS.between(2028-02-29, 2029-01-01) = 307일 → 307/365*15=12.6->CEIL=13(윤년 2/29 기산 정상 처리 확인용, 실측치로 고정)
        assertDays(13, totals, "STATUTORY_ANNUAL");
    }
}
