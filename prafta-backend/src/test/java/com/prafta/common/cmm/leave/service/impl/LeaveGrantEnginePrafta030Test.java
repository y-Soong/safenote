package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        when(dash.countActiveUser(anyString(), anyString())).thenReturn(1);
        when(dash.countLeaveTypeExists(anyString(), anyString())).thenReturn(1);
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(0);
        when(dash.countByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countActiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countActiveBySuffixVariant(anyString(), anyString())).thenReturn(0);
        when(dash.selectStatutoryGrantAccrual(anyString(), anyString(), anyString())).thenReturn(BigDecimal.ZERO);
        when(eng.selectLatestUnappliedHandling(anyString(), anyString())).thenReturn(null);
        when(eng.selectActiveStatutoryGrantIds(anyString(), anyString())).thenReturn(new ArrayList<>());
        final AtomicInteger seq = new AtomicInteger();
        when(dash.selectNextGrantId(anyString())).thenAnswer(inv -> "G" + seq.incrementAndGet());
    }
}
