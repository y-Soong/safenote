package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.mapper.LeaveGrantEngineMapper;
import com.prafta.common.cmm.leave.service.LeaveGrantStatusService;
import com.prafta.common.cmm.leave.service.LeavePolicyService;

/**
 * 경력인정 이원화 Phase 2 §2-1 — 입사일 기준 "정답" 누적 계산기({@code computeHireBasisAccrual}) 단위테스트.
 *
 * <p>단일 출처: {@code .claude/refs/연차_회계연도_비례부여_타임라인.md} §3.1(산식)·§4(2025-07-12 입사 10년 표).
 * 법정 기본 AXIS5(3년차 시작/2년 간격/25일 캡)로 활성 정책 null 을 그대로 사용한다(타임라인 표 전제와 동일).
 *
 * <p><b>★기대값 갱신 근거(P2-D1 재작업, 2026-08-22)</b>: 타임라인 §4 표는 <b>본연차 트랙만의 표</b>다
 * (§3.1 "월차는 비교 대상에서 제외(별도 트랙)"). 그러나 지시서 §2-1 은 정답 누적을 "월차+본연차+근속가산"
 * 합산으로 정의하므로, 경력 0 근로자의 각 기념일 정답 누적은 §4 표 값에 <b>기발생 월차 11일 오프셋(+11)</b>이
 * 붙는 것이 정합이다(QA P2-D1 실측: 20260711→11.0 / 20260712→26.0). 종전 기대값(§4 표 그대로 = 월차 미포함)은
 * 1주년 시점에 기발생 월차가 정답 트랙에서 통째로 사라지는 결함(High)을 그대로 고정하고 있었다.
 */
class LeaveGrantEngineHireBasisAccrualTest {

    private static final String CMPNY = "C001";
    private static final String USER = "U001";
    private static final String HIRE_YMD = "20250712";
    private static final LocalDate HIRE = LocalDate.of(2025, 7, 12);

    private LeaveDashboardMapper dash;
    private LeaveGrantEngineServiceImpl svc;

    @BeforeEach
    void setUp() {
        dash = mock(LeaveDashboardMapper.class);
        LeaveGrantEngineMapper eng = mock(LeaveGrantEngineMapper.class);
        LeavePolicyService policySvc = mock(LeavePolicyService.class);
        LeaveGrantStatusService statusSvc = mock(LeaveGrantStatusService.class);
        svc = new LeaveGrantEngineServiceImpl(dash, eng, policySvc, statusSvc);

        when(dash.selectUserHireDate(anyString(), anyString())).thenReturn(HIRE_YMD);
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(0);
        when(policySvc.findActivePolicy(anyString())).thenReturn(null); // 법정 기본 AXIS5 적용
    }

    private BigDecimal accrualAt(LocalDate baseDate) {
        return svc.computeHireBasisAccrual(CMPNY, USER, baseDate);
    }

    private void assertAccrual(long expected, LocalDate baseDate) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(accrualAt(baseDate)),
                "기준일 " + baseDate + " 정답 누적 기대 " + expected + ", 실제 " + accrualAt(baseDate));
    }

    // ============ 타임라인 §4 표(본연차 트랙) + 월차 11 오프셋: 2025-07-12 입사 10년 ============
    // §4 표: 15/30/46/62/79/96/114/132/151/170  →  월차 포함 정답 누적: +11 = 26/41/57/73/90/107/125/143/162/181

    @Test
    @DisplayName("1주년(2026-07-12) — 정답 누적 26일(월차 11 + 본연차 15, QA P2-D1 실측 기준)")
    void anniversary1_26days() {
        assertAccrual(26, HIRE.plusYears(1));
    }

    @Test
    @DisplayName("2주년(2027-07-12) — 정답 누적 41일(§4 표 30 + 월차 11)")
    void anniversary2_41days() {
        assertAccrual(41, HIRE.plusYears(2));
    }

    @Test
    @DisplayName("3주년(2028-07-12) — 정답 누적 57일(§4 표 46 + 월차 11, 근속가산 3년차 +1 반영)")
    void anniversary3_57days() {
        assertAccrual(57, HIRE.plusYears(3));
    }

    @Test
    @DisplayName("4주년(2029-07-12) — 정답 누적 73일(§4 표 62 + 월차 11)")
    void anniversary4_73days() {
        assertAccrual(73, HIRE.plusYears(4));
    }

    @Test
    @DisplayName("5주년(2030-07-12) — 정답 누적 90일(§4 표 79 + 월차 11)")
    void anniversary5_90days() {
        assertAccrual(90, HIRE.plusYears(5));
    }

    @Test
    @DisplayName("6주년(2031-07-12) — 정답 누적 107일(§4 표 96 + 월차 11)")
    void anniversary6_107days() {
        assertAccrual(107, HIRE.plusYears(6));
    }

    @Test
    @DisplayName("7주년(2032-07-12) — 정답 누적 125일(§4 표 114 + 월차 11)")
    void anniversary7_125days() {
        assertAccrual(125, HIRE.plusYears(7));
    }

    @Test
    @DisplayName("8주년(2033-07-12) — 정답 누적 143일(§4 표 132 + 월차 11)")
    void anniversary8_143days() {
        assertAccrual(143, HIRE.plusYears(8));
    }

    @Test
    @DisplayName("9주년(2034-07-12) — 정답 누적 162일(§4 표 151 + 월차 11)")
    void anniversary9_162days() {
        assertAccrual(162, HIRE.plusYears(9));
    }

    @Test
    @DisplayName("10주년(2035-07-12) — 정답 누적 181일(§4 표 170 + 월차 11, 10년 검증 최종행)")
    void anniversary10_181days() {
        assertAccrual(181, HIRE.plusYears(10));
    }

    // ============ 경계값(Q-4 부호/오프바이원 회귀 + P2-D1 실측 재현) ============

    @Test
    @DisplayName("입사일 이전 기준일 — 0")
    void beforeHire_zero() {
        assertAccrual(0, HIRE.minusDays(1));
    }

    @Test
    @DisplayName("입사 당일 — 0 (월차 미발생)")
    void hireDay_zero() {
        assertAccrual(0, HIRE);
    }

    @Test
    @DisplayName("입사 11개월 시점 — 월차 11일(캡)")
    void month11_monthlyEleven() {
        assertAccrual(11, HIRE.plusMonths(11));
    }

    @Test
    @DisplayName("1주년 하루 전(2026-07-11, QA 실측 11.0) — 아직 본연차 미발생, 월차 11일 유지(오프바이원 회귀 방지)")
    void oneDayBeforeAnniversary_stillMonthlyEleven() {
        assertAccrual(11, HIRE.plusYears(1).minusDays(1));
    }

    @Test
    @DisplayName("1주년 정확히 그날(2026-07-12, QA 실측 정답 26) — 기발생 월차 11이 사라지지 않고 본연차 15가 더해진다"
            + "(P2-D1 재작업 핵심: 11 → 26 점프, 종전 결함은 15로 떨어졌음)")
    void exactAnniversary_jumpsToTwentySix() {
        assertAccrual(26, HIRE.plusYears(1));
    }

    // ============ 반영 모드 경력인정 — 월차 월 단위 게이트(P2-D1) + 본연차 귀속 가속 ============
    // 엔진 실부여(computeMonthlyPeriods + isCreditDoubleDip)와 동일 의미: k번째 월차(발생일=입사+k개월)는
    //   발생 시점 산정근속(k+creditMonths)<12 일 때만 발생 ⇔ 발생 개월수 = max(0, min(실근속, 11, 11-credit)).
    //   credit 6 이면 k=1..5(산정근속 7..11)만 발생, k=6(산정근속 12 도달)부터 중단 = 총 5일.
    //   (엔진 검증: 게이트는 산정근속 12 도달일부터 목록 전체를 차단하므로, 그 직전(실근속 5개월 구간)까지
    //    부여된 월차는 1..5뿐 — 정답 누적의 월별 판정과 누적 결과가 일치한다.)

    @Test
    @DisplayName("반영 모드 경력 6개월 — 실근속 5개월(산정근속 11): 월차 5일까지만 발생(엔진 게이트 직전 상태)")
    void credit6_atActual5Months_monthlyFive() {
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(6);
        assertAccrual(5, HIRE.plusMonths(5));
    }

    @Test
    @DisplayName("반영 모드 경력 6개월 — 실근속 6개월(산정근속 12): 월차 5일 유지 + 본연차 15 귀속 = 20"
            + "(종전 결함은 월차 0 + 15 = 15 — 기발생 월차 5일이 사라졌음)")
    void credit6_atActual6Months_monthlyKeptPlusAnnual() {
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(6);
        assertAccrual(20, HIRE.plusMonths(6));
    }

    @Test
    @DisplayName("반영 모드 경력 6개월 — 실근속 11개월: 월차는 5일에서 중단(게이트 이후 미발생), 본연차 15 = 20")
    void credit6_atActual11Months_monthlyStopsAtFive() {
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(6);
        assertAccrual(20, HIRE.plusMonths(11));
    }

    @Test
    @DisplayName("반영 모드 경력 24개월 — 월차 0(첫 달부터 산정근속 12 이상) + 귀속 2년차분 30"
            + "(QA Q-4(c) BOT11 실측 30.0과 동일 — P2-D1 수정으로도 불변이어야 한다)")
    void credit24_noMonthly_vestedTwoYears() {
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(24);
        // 실근속 1개월: 월차 = max(0, min(1, 11, 11-24)) = 0, 귀속연차 = (1+24)/12 = 2 → 15×2 = 30
        assertAccrual(30, HIRE.plusMonths(1));
    }
}
