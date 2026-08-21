package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.mapper.LeaveGrantEngineMapper;
import com.prafta.common.cmm.leave.service.LeaveGrantStatusService;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.leave.vo.HireDateGrantResultVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantInsertVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.cmm.leave.vo.PolicyGrantPreviewVO;

/**
 * prafta-030 — 입사일 변경 처리방식별 차액 보전(BE-1) + 월차 게이트(BE-2) 결정적 단위테스트.
 *
 * <p>단일 출처: {@code .claude/requests/prafta-030-decisions.md}(D1~D5),
 * 정답표 {@code .claude/requests/ref/prafta-030/_xlsx_dump.txt}(SHEET4 18케이스·SHEET7 경계A·SHEET8 경계B).
 *
 * <p>{@code mockStatic(LocalDate)}로 오늘=2026-05-26 고정, 회계연도 1/1, AXIS5 법정(3/2/25), AXIS6=12개월.
 * 매퍼/정책 서비스는 Mockito mock이다. 정답표는 "기존 부여 누적"을 전제로 하므로 2단계로 시뮬한다.
 * <ul>
 *   <li>(a) 변경 전 입사일 기준 "기존 부여 누적"을 {@code selectStatutoryGrantAccrual}(전 STATUTORY,
 *       소멸제외·사용포함, 월차 포함 — 정정 2026-05-26) mock으로 SHEET2/SHEET8 값으로 주입한다.</li>
 *   <li>(b) 변경 후 입사일 + 미적용 이력(handlingType)으로 부여/프리뷰 호출 → 추가/최종 검증.</li>
 * </ul>
 *
 * <p>BE-1 핵심: KEEP_AND_BACKFILL은 당기/컴포넌트 백필을 끄고 "차액 단건"(GRANT_TYPE=STATUTORY_ANNUAL,
 * GRANT_REASON=입사일 변경 보전, 멱등키 접미사 _BF{histId})만 부여한다. 차액 ≤ 0이면 보전 없음(옵션2 동치).
 */
class LeaveGrantEnginePrafta030Test {

    /** 고정 "오늘" = prafta-030 정답표 기준일. */
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 26);
    private static final String CMPNY = "C001";
    private static final String USER = "U001";
    private static final String MGR = "master";
    private static final String OP = "OP001";

    private static final String GRANT_TYPE_ANNUAL = "STATUTORY_ANNUAL";
    private static final String GRANT_TYPE_MONTHLY = "STATUTORY_MONTHLY";
    // 경력인정 이원화(2026-08-21, 지시서 §1-4) — 일수 모드 연간 자동 부여.
    private static final String GRANT_TYPE_CAREER = "MANUAL_CAREER";
    private static final String LEAVE_CD_CAREER = "SYS_CAREER";

    private LeaveDashboardMapper dash;
    private LeaveGrantEngineMapper eng;
    private LeavePolicyService policySvc;
    private LeaveGrantStatusService statusSvc;
    private LeaveGrantEngineServiceImpl svc;
    private MockedStatic<LocalDate> localDateMock;

    @BeforeEach
    void setUp() {
        localDateMock = mockStatic(LocalDate.class, CALLS_REAL_METHODS);
        localDateMock.when(LocalDate::now).thenReturn(TODAY);

        dash = mock(LeaveDashboardMapper.class);
        eng = mock(LeaveGrantEngineMapper.class);
        policySvc = mock(LeavePolicyService.class);

        // ★소정-05 게이트 스텁 (2026-08-19 추가): prepareGrantContext 진입부가
        //   isStatutoryAutoGrantEnabled(policy)=false 면 LEAVE_400_001 로 전면 차단한다.
        //   실제 구현은 '값이 N 이 아니면 통과'라 기본 통과지만, mock 기본값은 false 라
        //   스텁이 없으면 모든 시나리오가 계산 이전에 튕긴다(테스트 전멸의 원인).
        when(policySvc.isStatutoryAutoGrantEnabled(any(LeavePolicyVO.class))).thenReturn(true);
        when(policySvc.isStatutoryAutoGrantEnabled(anyString())).thenReturn(true);
        statusSvc = mock(LeaveGrantStatusService.class);
        svc = new LeaveGrantEngineServiceImpl(dash, eng, policySvc, statusSvc);

        when(dash.countActiveUser(anyString(), anyString())).thenReturn(1);
        when(dash.countLeaveTypeExists(anyString(), anyString())).thenReturn(1);
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(0);
        when(dash.countByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countActiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countActiveBySuffixVariant(anyString(), anyString())).thenReturn(0);
        // 기존 부여누적 기본값 0 (각 케이스가 SHEET 값으로 덮어씀). prafta-030 정정: 소멸제외·사용포함 + 월차 포함(3-arg).
        when(dash.selectStatutoryGrantAccrual(anyString(), anyString(), anyString())).thenReturn(BigDecimal.ZERO);
        // 경력인정 이원화(2026-08-21) — 일수 모드 credit 합계 기본값 0(각 케이스가 필요 시 덮어씀).
        when(dash.selectExtraLeaveDaysSum(anyString(), anyString())).thenReturn(BigDecimal.ZERO);
        when(eng.selectLatestUnappliedHandling(anyString(), anyString())).thenReturn(null);
        when(eng.selectActiveStatutoryGrantIds(anyString(), anyString())).thenReturn(List.of());
        final AtomicInteger seq = new AtomicInteger();
        when(dash.selectNextGrantId(anyString())).thenAnswer(inv -> "G" + seq.incrementAndGet());
    }

    @AfterEach
    void tearDown() {
        localDateMock.close();
    }

    // ============================ policy builders ============================

    private LeavePolicyVO hirePolicy() {
        LeavePolicyVO p = new LeavePolicyVO();
        p.setPolicySeq(1L);
        p.setAxis1GrantBase("HIRE_DATE");
        p.setAxis3FirstYearMethod("MONTHLY_ONLY");
        p.setAxis6ValidityMonths(12);
        return p;
    }

    private LeavePolicyVO fiscalPolicy(String method, String rounding) {
        LeavePolicyVO p = new LeavePolicyVO();
        p.setPolicySeq(1L);
        p.setAxis1GrantBase("FISCAL_YEAR");
        p.setAxis2FiscalStartMm("01");
        p.setAxis2FiscalStartDd("01");
        p.setAxis3FirstYearMethod(method);
        p.setAxis4ProrateRounding(rounding);
        p.setAxis6ValidityMonths(12);
        return p;
    }

    // ============================ 2단계 시뮬 하니스 ============================

    /** 차액 보전 단건의 부여 결과(추가일수 + 멱등키 + 소멸일). */
    private static final class BackfillGrant {
        final BigDecimal addDays;
        final String idempotencyKey;
        final String availToDate;
        final String grantReason;

        BackfillGrant(BigDecimal addDays, String key, String availTo, String reason) {
            this.addDays = addDays;
            this.idempotencyKey = key;
            this.availToDate = availTo;
            this.grantReason = reason;
        }
    }

    /**
     * KEEP_AND_BACKFILL 옵션1 apply 실행 + 차액 보전 단건 캡처.
     *
     * @param policy           활성 정책
     * @param newHire          변경 후 입사일(YYYYMMDD)
     * @param existingAnnualSum 기존 부여누적(전 STATUTORY, 소멸제외·사용포함, 월차 포함 — 정정 2026-05-26) — SHEET2/SHEET8 값
     * @return 보전 단건(STATUTORY_ANNUAL, _BF 접미사). 보전 없으면 addDays=0/key=null.
     */
    private BackfillGrant applyBackfill(LeavePolicyVO policy, String newHire, BigDecimal existingAnnualSum) {
        when(policySvc.findActivePolicy(anyString())).thenReturn(policy);
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn(newHire);
        when(eng.selectLatestUnappliedHandling(eq(CMPNY), eq(USER)))
                .thenReturn(Map.of("HANDLING_TYPE", "KEEP_AND_BACKFILL", "HIST_ID", "H1"));
        when(dash.selectStatutoryGrantAccrual(eq(CMPNY), eq(USER), anyString())).thenReturn(existingAnnualSum);

        HireDateGrantResultVO r = svc.hireDateGrant(CMPNY, List.of(USER), MGR, OP);

        ArgumentCaptor<LeaveGrantInsertVO> cap = ArgumentCaptor.forClass(LeaveGrantInsertVO.class);
        verify(dash, atLeast(0)).insertManualGrant(cap.capture());
        for (LeaveGrantInsertVO vo : cap.getAllValues()) {
            // 차액 보전 단건 = STATUTORY_ANNUAL + _BF 접미사 멱등키
            if (GRANT_TYPE_ANNUAL.equals(vo.getGrantType())
                    && vo.getIdempotencyKey() != null && vo.getIdempotencyKey().contains("_BF")) {
                return new BackfillGrant(vo.getGrantDays(), vo.getIdempotencyKey(),
                        vo.getAvailToDate(), vo.getGrantReason());
            }
        }
        // 보전 없음
        assertTrue(r.getGrantedDays() != null, "결과 VO non-null");
        return new BackfillGrant(BigDecimal.ZERO, null, null, null);
    }

    /** apply 실행 후 STATUTORY_MONTHLY INSERT 건수(월차 게이트 검증용). */
    private int countMonthlyInserts() {
        ArgumentCaptor<LeaveGrantInsertVO> cap = ArgumentCaptor.forClass(LeaveGrantInsertVO.class);
        verify(dash, atLeast(0)).insertManualGrant(cap.capture());
        int n = 0;
        for (LeaveGrantInsertVO vo : cap.getAllValues()) {
            if (GRANT_TYPE_MONTHLY.equals(vo.getGrantType())) {
                n++;
            }
        }
        return n;
    }

    private static int days(BigDecimal d) {
        return d == null ? 0 : d.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
    }

    // ============================ SHEET4: 옵션1 미래변경 (차액 0 — 줄이지 않음) ============================
    // (prafta-032 009: 처리방식 차액보전 @Disabled 테스트 case1/case7/case13 물리 삭제 —
    //  부여 엔진의 backfill 분기가 제거되어 검증 대상이 사라짐. 미래변경 케이스는 신규 부여만이라 유효해 보존.)

    @Test
    @DisplayName("#2 HIRE_DATE 옵션1 미래(2023->2025): 새15 ≤ 기존16 → 차액 +0, 최종 16(유리한 쪽 유지)")
    void case2_hireDateFutureNoReduce() {
        // 새 기준 2025-01-01: 1년차 본연차15(가산0). 기존 누적 16.
        BackfillGrant bf = applyBackfill(hirePolicy(), "20250101", BigDecimal.valueOf(16));
        assertEquals(0, days(bf.addDays), "차액 0 (15-16 ≤ 0 → 추가 없음)");
        assertEquals(16, 16 + days(bf.addDays), "최종 보유 16(기존 유지)");
    }

    @Test
    @DisplayName("#8 FISCAL+PRORATE 옵션1 미래(->2025): 새15 = 기존15 → 차액 +0, 최종 15")
    void case8_fiscalProrateFutureNoReduce() {
        BackfillGrant bf = applyBackfill(fiscalPolicy("PRORATE", "CEIL"), "20250101", BigDecimal.valueOf(15));
        assertEquals(0, days(bf.addDays), "차액 0 (15-15)");
        assertEquals(15, 15 + days(bf.addDays), "최종 보유 15");
    }

    @Test
    @DisplayName("#14 FISCAL+NEXT_YEAR_BULK 옵션1 미래(->2025): 새15 = 기존15 → 차액 +0, 최종 15")
    void case14_fiscalBulkFutureNoReduce() {
        BackfillGrant bf = applyBackfill(fiscalPolicy("NEXT_YEAR_BULK", "CEIL"), "20250101", BigDecimal.valueOf(15));
        assertEquals(0, days(bf.addDays), "차액 0 (15-15)");
        assertEquals(15, 15 + days(bf.addDays), "최종 보유 15");
    }

    // ============================ BE-2 월차 게이트 (본연차 발생 시 월차 0) ============================
    // (prafta-032 009: @Disabled boundaryB_monthlyToAnnualBackfillPlusEight 물리 삭제 — 차액보전 분기 제거.)

    @Test
    @DisplayName("월차 게이트: #1(HIRE 2021)·#7/#13(FISCAL 2021)·경계B(2023-10-01) 모두 신규 월차 0건")
    void monthlyGate_noMonthlyWhenAnnualAccrued() {
        // #1
        applyBackfill(hirePolicy(), "20210101", BigDecimal.valueOf(16));
        assertEquals(0, countMonthlyInserts(), "#1 본연차 발생 → 월차 0");
        resetMocks();
        // #7
        applyBackfill(fiscalPolicy("PRORATE", "CEIL"), "20210101", BigDecimal.valueOf(15));
        assertEquals(0, countMonthlyInserts(), "#7 FISCAL crossed>=1 → 월차 0");
        resetMocks();
        // #13
        applyBackfill(fiscalPolicy("NEXT_YEAR_BULK", "CEIL"), "20210101", BigDecimal.valueOf(15));
        assertEquals(0, countMonthlyInserts(), "#13 FISCAL crossed>=1 → 월차 0");
    }

    @Test
    @DisplayName("월차 게이트: 본연차 미발생(HIRE 4개월·FISCAL crossed==0)은 월차 유지(공백 방지)")
    void monthlyGate_keepsMonthlyWhenNoAnnual() {
        // HIRE_DATE 입사 2026-02-01(만 ~3개월, 본연차 미발생) → 월차 유지. APPLY_NEW(이력 없음).
        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy());
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20260201");
        when(eng.selectLatestUnappliedHandling(eq(CMPNY), eq(USER))).thenReturn(null);
        svc.hireDateGrant(CMPNY, List.of(USER), MGR, OP);
        assertTrue(countMonthlyInserts() > 0, "본연차 미발생(HIRE 4개월) → 월차 유지");

        resetMocks();
        // FISCAL 입사 2026-02-01: crossed==0(2026-01-01은 입사 전) → 본연차 미발생 → 월차 유지.
        when(policySvc.findActivePolicy(anyString())).thenReturn(fiscalPolicy("NEXT_YEAR_BULK", "CEIL"));
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20260201");
        when(eng.selectLatestUnappliedHandling(eq(CMPNY), eq(USER))).thenReturn(null);
        svc.hireDateGrant(CMPNY, List.of(USER), MGR, OP);
        assertTrue(countMonthlyInserts() > 0, "FISCAL crossed==0 → 월차 유지(공백 방지)");
    }

    /**
     * ★2026-08-20 정정 회귀 — 월차 게이트 판정을 <b>산정근속 도달 시점</b>으로 통일.
     *
     * <p>종전에는 "이번 부여에 full 본연차 15 발생"이 AND 조건이라, FISCAL 축에서 {@code crossed==0} 인
     * 동안은 경력인정으로 산정근속이 1년을 넘겨도 월차가 계속 발생했다(같은 사람이 AXIS1 축에 따라 갈림).
     * 이 테스트가 실패하면 그 조건이 되살아난 것이다.
     */
    @Test
    @DisplayName("월차 게이트(정정): 경력인정으로 산정근속 1년 도달 → AXIS1 무관 월차 차단, 부분 인정(<1년)은 유지")
    void monthlyGate_blocksOnCreditedTenureRegardlessOfAxis() {
        // (A) HIRE_DATE — 입사 2025-11-26(실근속 6개월) + 경력인정 6개월 = 산정근속 12 → 차단.
        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy());
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20251126");
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(6);
        svc.hireDateGrant(CMPNY, List.of(USER), MGR, OP);
        assertEquals(0, countMonthlyInserts(), "(A) HIRE_DATE 산정근속 12개월 → 월차 차단");

        resetMocks();
        // (B) ★핵심: FISCAL crossed==0 — 입사 2026-02-01(실근속 3개월, 2026-01-01은 입사 전이라 미도래)
        //     + 경력인정 9개월 = 산정근속 12 → 본연차는 아직 없지만(부여 시점 미도래) 월차는 차단한다.
        //     종전 규칙에서는 여기서 월차 3개가 발생했다.
        when(policySvc.findActivePolicy(anyString())).thenReturn(fiscalPolicy("NEXT_YEAR_BULK", "CEIL"));
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20260201");
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(9);
        svc.hireDateGrant(CMPNY, List.of(USER), MGR, OP);
        assertEquals(0, countMonthlyInserts(), "(B) FISCAL crossed==0 이어도 산정근속 12개월 → 월차 차단");

        resetMocks();
        // (C) 무회귀 — 부분 경력인정(산정근속 8개월 < 12)은 게이트 비대상이라 월차가 그대로 발생.
        when(policySvc.findActivePolicy(anyString())).thenReturn(fiscalPolicy("NEXT_YEAR_BULK", "CEIL"));
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20260201");
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(5);
        svc.hireDateGrant(CMPNY, List.of(USER), MGR, OP);
        assertTrue(countMonthlyInserts() > 0, "(C) 산정근속 8개월 → 월차 유지(게이트 비대상)");
    }

    // ============================ 옵션2(APPLY_NEW)·옵션3(RESET_ALL) 회귀 ============================
    // (prafta-032 009: @Disabled backfill_idempotentReclick / preview_backfillShortfallReflected 물리 삭제 —
    //  차액보전 부여·preview 반영 분기가 부여 엔진에서 제거되어 검증 대상이 사라짐.)

    @Test
    @DisplayName("#3 옵션2(APPLY_NEW) 과거변경: 당기 멱등 → 추가 0(현재 그대로)")
    void case3_applyNewPastNoAdd() {
        // 옵션2: 기존 보유 유지, 당기 ANNUAL/TENURE는 이미 부여됨(live) → 신규 0.
        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy());
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20210101");
        when(eng.selectLatestUnappliedHandling(eq(CMPNY), eq(USER)))
                .thenReturn(Map.of("HANDLING_TYPE", "KEEP_AND_APPLY_NEW", "HIST_ID", "H1"));
        // 당기 부여가 이미 live(라벨 2026 본연차/가산 기부여)
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(1);
        PolicyGrantPreviewVO p = svc.previewPolicyGrant(CMPNY, List.of(USER), MGR);
        assertEquals(0, p.getRows().get(0).getAddDays(), "옵션2 과거변경 추가 0");
    }

    private void resetMocks() {
        org.mockito.Mockito.reset(dash, eng, policySvc, statusSvc);
        // ★reset 은 setUp 의 소정-05 게이트 스텁까지 지운다 → 여기서 즉시 복원(없으면 LEAVE_400_001 전멸).
        when(policySvc.isStatutoryAutoGrantEnabled(any(LeavePolicyVO.class))).thenReturn(true);
        when(policySvc.isStatutoryAutoGrantEnabled(anyString())).thenReturn(true);
        when(dash.countActiveUser(anyString(), anyString())).thenReturn(1);
        when(dash.countLeaveTypeExists(anyString(), anyString())).thenReturn(1);
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(0);
        when(dash.countByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countActiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countActiveBySuffixVariant(anyString(), anyString())).thenReturn(0);
        when(dash.selectStatutoryGrantAccrual(anyString(), anyString(), anyString())).thenReturn(BigDecimal.ZERO);
        when(dash.selectExtraLeaveDaysSum(anyString(), anyString())).thenReturn(BigDecimal.ZERO);
        when(eng.selectLatestUnappliedHandling(anyString(), anyString())).thenReturn(null);
        when(eng.selectActiveStatutoryGrantIds(anyString(), anyString())).thenReturn(new ArrayList<>());
        final AtomicInteger seq = new AtomicInteger();
        when(dash.selectNextGrantId(anyString())).thenAnswer(inv -> "G" + seq.incrementAndGet());
    }

    // ============================ 경력인정 이원화(2026-08-21, 지시서 §1) — Phase 1 신규 케이스 ============================

    /** apply 실행 후 특정 GRANT_TYPE 의 INSERT 건수. */
    private int countInsertsByType(String grantType) {
        ArgumentCaptor<LeaveGrantInsertVO> cap = ArgumentCaptor.forClass(LeaveGrantInsertVO.class);
        verify(dash, atLeast(0)).insertManualGrant(cap.capture());
        int n = 0;
        for (LeaveGrantInsertVO vo : cap.getAllValues()) {
            if (grantType.equals(vo.getGrantType())) {
                n++;
            }
        }
        return n;
    }

    /** apply 실행 후 특정 GRANT_TYPE 의 첫 INSERT VO(없으면 null). */
    private LeaveGrantInsertVO firstInsertByType(String grantType) {
        ArgumentCaptor<LeaveGrantInsertVO> cap = ArgumentCaptor.forClass(LeaveGrantInsertVO.class);
        verify(dash, atLeast(0)).insertManualGrant(cap.capture());
        for (LeaveGrantInsertVO vo : cap.getAllValues()) {
            if (grantType.equals(vo.getGrantType())) {
                return vo;
            }
        }
        return null;
    }

    @Test
    @DisplayName("일수 모드: 반영 개월(selectCreditMonths, Y-필터) 0 + 일수 모드 합계 3.5 → 월차 정상 발생 + MANUAL_CAREER 3.5일 부여")
    void daysMode_monthlyStaysNormal_andCareerGranted() {
        // 입사 2025-11-26 → 오늘(2026-05-26) 기준 실근속 6개월. 일수 모드는 selectCreditMonths(Y-필터 SQL)에
        // 잡히지 않으므로(정책 P-7) 0으로 스텁 — 산정근속도 6개월(<12)이라 월차 게이트 비대상.
        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy());
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20251126");
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(0);
        when(dash.selectExtraLeaveDaysSum(anyString(), anyString())).thenReturn(BigDecimal.valueOf(3.5));

        svc.hireDateGrant(CMPNY, List.of(USER), MGR, OP);

        assertTrue(countInsertsByType(GRANT_TYPE_MONTHLY) > 0, "일수 모드는 산정근속 미가산 → 월차 정상 발생");
        assertEquals(0, countInsertsByType(GRANT_TYPE_ANNUAL), "실근속 6개월 미만이라 본연차는 미발생(정상)");

        LeaveGrantInsertVO career = firstInsertByType(GRANT_TYPE_CAREER);
        assertNotNull(career, "MANUAL_CAREER 컴포넌트가 정기부여 배치에 편입되어야 한다(T-1)");
        assertEquals(0, BigDecimal.valueOf(3.5).compareTo(career.getGrantDays()), "부여량=일수 모드 합계 3.5");
        assertEquals(LEAVE_CD_CAREER, career.getLeaveCd());
        assertTrue(career.getIdempotencyKey() != null
                        && career.getIdempotencyKey().startsWith(USER + "_")
                        && career.getIdempotencyKey().endsWith("_" + GRANT_TYPE_CAREER),
                () -> "멱등키 형식 = {userCd}_{periodLabel}_MANUAL_CAREER, 실제=" + career.getIdempotencyKey());
    }

    @Test
    @DisplayName("혼합 보유자: 반영 모드(월차 게이트+본연차 가산)와 일수 모드(MANUAL_CAREER)가 한 사용자에게 동시 적용")
    void mixedHolder_reflectGatesMonthly_andDaysGrantsCareerIndependently() {
        // 반영 모드 15개월 가산 → 산정근속 6+15=21(>=12) → 월차 게이트 발동 + 본연차 15일 발생.
        // 동시에 일수 모드 2.5일도 등록되어 있어 MANUAL_CAREER 는 반영 모드 가산과 무관하게 별도 부여된다.
        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy());
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20251126");
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(15);
        when(dash.selectExtraLeaveDaysSum(anyString(), anyString())).thenReturn(BigDecimal.valueOf(2.5));

        svc.hireDateGrant(CMPNY, List.of(USER), MGR, OP);

        assertEquals(0, countInsertsByType(GRANT_TYPE_MONTHLY), "반영 모드 산정근속 12개월 도달 → 월차 게이트 발동");
        LeaveGrantInsertVO annual = firstInsertByType(GRANT_TYPE_ANNUAL);
        assertNotNull(annual, "반영 모드 가산으로 산정근속 12개월 이상 → 본연차 발생");
        assertEquals(0, BigDecimal.valueOf(15).compareTo(annual.getGrantDays()));

        LeaveGrantInsertVO career = firstInsertByType(GRANT_TYPE_CAREER);
        assertNotNull(career, "일수 모드 합계가 있으면 반영 모드 여부와 무관하게 MANUAL_CAREER 부여");
        assertEquals(0, BigDecimal.valueOf(2.5).compareTo(career.getGrantDays()));
    }

    @Test
    @DisplayName("MANUAL_CAREER 정기부여 - 시스템 연차 종류(SYS_CAREER) 미설정 회사는 skip(로그만, 다른 부여는 정상 진행)")
    void careerMode_missingLeaveType_skipsWithoutBlockingOthers() {
        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy());
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20210101"); // 장기 근속 → 본연차+가산 발생
        when(dash.selectExtraLeaveDaysSum(anyString(), anyString())).thenReturn(BigDecimal.valueOf(3));
        // SYS_CAREER 만 미설정, 그 외(SYS_ANNUAL/MONTHLY/TENURE)는 기본 스텁(1)대로 존재.
        when(dash.countLeaveTypeExists(eq(CMPNY), eq(LEAVE_CD_CAREER))).thenReturn(0);

        svc.hireDateGrant(CMPNY, List.of(USER), MGR, OP);

        assertEquals(0, countInsertsByType(GRANT_TYPE_CAREER), "SYS_CAREER 미설정이면 MANUAL_CAREER는 skip");
        assertNotNull(firstInsertByType(GRANT_TYPE_ANNUAL), "SYS_CAREER 미설정이 본연차 부여까지 막으면 안 된다(throw 금지)");
    }

    @Test
    @DisplayName("P-8 즉시 부여 → 정기부여 배치 재실행 시 이중 생성 0건(동일 멱등키 공유)")
    void immediateGrant_thenScheduledBatch_noDoubleGrant() {
        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy());
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20251126");
        when(dash.selectExtraLeaveDaysSum(anyString(), anyString())).thenReturn(BigDecimal.valueOf(3));

        // (1) 경력 인정 등록 즉시 부여(P-8) — User01ServiceImpl 이 호출하는 경로.
        svc.grantManualCareerImmediate(CMPNY, USER, OP);

        LeaveGrantInsertVO firstGrant = firstInsertByType(GRANT_TYPE_CAREER);
        assertNotNull(firstGrant, "즉시 부여 1건이 생성되어야 한다");
        assertEquals(1, countInsertsByType(GRANT_TYPE_CAREER));

        // (2) 방금 부여된 멱등키가 이제 live 상태라고 가정(실제로는 DB 상태) — 정기부여 배치가 재실행돼도
        //     같은 키로 alreadyGranted() 가 true 를 반환해 재INSERT 되지 않아야 한다(P-8 이중생성 차단).
        when(dash.countLiveByIdempotencyKey(eq(CMPNY), eq(firstGrant.getIdempotencyKey()))).thenReturn(1);

        svc.hireDateGrant(CMPNY, List.of(USER), MGR, OP);

        assertEquals(1, countInsertsByType(GRANT_TYPE_CAREER), "정기부여 배치 재실행 후에도 MANUAL_CAREER 는 여전히 1건(이중 생성 없음)");
    }

    @Test
    @DisplayName("P-9 게이트: 즉시 부여 경로는 법정 자동부여 OFF 회사에서 skip(예외 없음, throw 금지)")
    void immediateGrant_autoGrantOff_skipsWithoutThrow() {
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20251126");
        when(dash.selectExtraLeaveDaysSum(anyString(), anyString())).thenReturn(BigDecimal.valueOf(3));
        // 이 회사만 소정-05 OFF.
        when(policySvc.isStatutoryAutoGrantEnabled(eq(CMPNY))).thenReturn(false);

        assertDoesNotThrow(() -> svc.grantManualCareerImmediate(CMPNY, USER, OP),
                "등록 트랜잭션을 롤백시키지 않도록 throw 하지 않아야 한다(R-5)");
        assertEquals(0, countInsertsByType(GRANT_TYPE_CAREER), "OFF 회사는 실제 부여를 skip");
    }

    @Test
    @DisplayName("즉시 부여: 일수 모드 합계가 0이면 무처리(반영 모드만 등록한 경우 등)")
    void immediateGrant_zeroSum_noOp() {
        // selectExtraLeaveDaysSum 기본값(setUp)=0 그대로 사용.
        assertDoesNotThrow(() -> svc.grantManualCareerImmediate(CMPNY, USER, OP));
        assertEquals(0, countInsertsByType(GRANT_TYPE_CAREER));
    }

    // ============================ D-1 재작업(2026-08-21, qa-report) — 프리뷰/적용 정합 ============================

    @Test
    @DisplayName("D-1: previewPolicyGrant 가 MANUAL_CAREER 를 집계한다(프리뷰=적용 정합, 별도 산식 없이 동일 로직 재사용)")
    void preview_includesManualCareer_matchesApply() {
        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy());
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20210101");
        when(dash.selectExtraLeaveDaysSum(anyString(), anyString())).thenReturn(BigDecimal.valueOf(4));
        // 본연차/가산/월차는 이미 당기 부여 완료(live) — MANUAL_CAREER 만 아직 미부여인 상태를 재현(qa D-1 실측 그대로).
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(1);
            return key.contains(GRANT_TYPE_CAREER) ? 0 : 1;
        });

        PolicyGrantPreviewVO preview = svc.previewPolicyGrant(CMPNY, List.of(USER), MGR);
        assertEquals(4, preview.getRows().get(0).getAddDays(),
                "프리뷰가 MANUAL_CAREER 합계(4일)를 addDays 에 반영해야 한다(수정 전엔 0으로 누락됨)");
        assertTrue(preview.getRows().get(0).getNote() != null
                        && preview.getRows().get(0).getNote().contains("경력인정"),
                "note 에 경력인정 표기가 있어야 한다(월차와 동일한 방식으로 안내)");

        // 적용(hireDateGrant)도 동일 4일을 실제로 부여해야 한다 — qa D-1 재현 "프리뷰 0 vs 적용 4" 불일치 해소 확인.
        svc.hireDateGrant(CMPNY, List.of(USER), MGR, OP);
        LeaveGrantInsertVO career = firstInsertByType(GRANT_TYPE_CAREER);
        assertNotNull(career, "MANUAL_CAREER 가 실제로 부여되어야 한다");
        assertEquals(0, BigDecimal.valueOf(4).compareTo(career.getGrantDays()),
                "프리뷰 집계량과 실제 부여량이 일치해야 한다(프리뷰/적용 불일치 해소)");
    }

    @Test
    @DisplayName("D-1: SYS_CAREER 미설정 회사는 프리뷰에서도 MANUAL_CAREER 를 집계하지 않는다(적용과 동일 skip 조건 공유)")
    void preview_skipsManualCareer_whenLeaveTypeMissing() {
        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy());
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20210101");
        when(dash.selectExtraLeaveDaysSum(anyString(), anyString())).thenReturn(BigDecimal.valueOf(4));
        when(dash.countLeaveTypeExists(eq(CMPNY), eq(LEAVE_CD_CAREER))).thenReturn(0);
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(1);

        PolicyGrantPreviewVO preview = svc.previewPolicyGrant(CMPNY, List.of(USER), MGR);
        assertEquals(0, preview.getRows().get(0).getAddDays(),
                "SYS_CAREER 미설정 회사는 프리뷰도 0 — 적용의 skip 조건(countLeaveTypeExists)과 동일하게 맞춰야 한다");
    }
}
