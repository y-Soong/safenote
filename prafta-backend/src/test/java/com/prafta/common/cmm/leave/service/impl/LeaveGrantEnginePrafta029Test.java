package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.prafta.common.cmm.leave.vo.PolicyGrantPreviewRowVO;
import com.prafta.common.cmm.leave.vo.PolicyGrantPreviewVO;

/**
 * prafta-029 leave grant engine full-matrix runner (deterministic, pure logic + Mockito).
 *
 * <p>Purpose: emit the engine's ACTUAL granted-day count for every prafta-029 matrix cell so QA
 * can compare against an independent policy-derived expectation. {@code LocalDate.now()} is pinned
 * to {@link #TODAY} (= 2026-05-25, the request's "current date"); mappers/policy service are Mockito
 * mocks. The granted-day count comes from {@code previewPolicyGrant().addDays} (== what apply would
 * INSERT for a fresh user with no prior grants — the agreed methodology).
 *
 * <p>Fixed policy parameters (confirmed with requester):
 * <ul>
 *   <li>Fiscal start = 01-01 (calendar year).</li>
 *   <li>PRORATE rounding (AXIS4) = CEIL. The runner also prints the raw (pre-ceil) proration so the
 *       report can state "c = ceil(rawN)".</li>
 *   <li>AXIS6 validity = 12 months (legal fixed, prafta-028). Past accruals whose 12-month validity
 *       already elapsed are excluded (expired).</li>
 *   <li>AXIS5 tenure = LEGAL default (start year 3 / interval 2 / max 25): +1 day from the 3rd year.</li>
 *   <li>Fresh user: countByIdempotencyKey = 0 (no prior grants).</li>
 * </ul>
 *
 * <p>This is a RUNNER, not an assertion-heavy regression: it asserts only that every cell executes
 * without error and yields a non-negative count. The printed table is the ground truth for QA.
 */
class LeaveGrantEnginePrafta029Test {

    /** Pinned "today" = prafta-029 current date. */
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 25);
    private static final String CMPNY = "C001";
    private static final String USER = "U001";
    private static final String MGR = "master";

    private static final int BASE_ANNUAL = 15;
    private static final int VALIDITY = 12;

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
        when(dash.countByIdempotencyKey(anyString(), anyString())).thenReturn(0);   // fresh user
        // prafta-029 옵션 A: alreadyGranted 가 live-only(countLiveByIdempotencyKey) 로 전환됨 → 기부여 판정 스텁도 함께.
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(0); // fresh user (no live grant)
        when(dash.countActiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(eng.selectLatestUnappliedHandling(anyString(), anyString())).thenReturn(null);
        when(eng.selectActiveStatutoryGrantIds(anyString(), anyString())).thenReturn(List.of());
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
        p.setAxis6ValidityMonths(VALIDITY);
        return p;
    }

    /** FISCAL_YEAR policy, fiscal start 01-01, given AXIS3 method + AXIS4 rounding. */
    private LeavePolicyVO fiscalPolicy(String method, String rounding) {
        LeavePolicyVO p = new LeavePolicyVO();
        p.setPolicySeq(1L);
        p.setAxis1GrantBase("FISCAL_YEAR");
        p.setAxis2FiscalStartMm("01");
        p.setAxis2FiscalStartDd("01");
        p.setAxis3FirstYearMethod(method);
        p.setAxis4ProrateRounding(rounding);
        p.setAxis6ValidityMonths(VALIDITY);
        return p;
    }

    // ============================ case model ============================

    private static final class Case {
        final String policyName;
        final LeavePolicyVO policy;
        final boolean fiscalProrate;
        final String group;   // A (<1yr base) / B (>1yr base)
        final String label;   // base / future1 / past1 ...
        final String hire;    // YYYYMMDD
        final String handling; // null=APPLY_NEW(no history) / KEEP_AND_BACKFILL / KEEP_AND_APPLY_NEW / RESET_ALL

        Case(String policyName, LeavePolicyVO policy, boolean fiscalProrate,
             String group, String label, String hire, String handling) {
            this.policyName = policyName;
            this.policy = policy;
            this.fiscalProrate = fiscalProrate;
            this.group = group;
            this.label = label;
            this.hire = hire;
            this.handling = handling;
        }
    }

    // group A: base <1yr (hire 2025-07-21); changed dates (2 future, 2 past relative to original)
    private static final String[][] GROUP_A = {
            {"base",    "20250721"},
            {"future1", "20251011"},
            {"past1",   "20250611"},
            {"future2", "20260102"},
            {"past2",   "20250222"},
    };
    // group B: base >1yr (hire 2023-07-21); one future, one past
    private static final String[][] GROUP_B = {
            {"base",   "20230721"},
            {"future", "20231211"},
            {"past",   "20230211"},
    };
    private static final String[] HANDLINGS = {null, "KEEP_AND_BACKFILL", "KEEP_AND_APPLY_NEW", "RESET_ALL"};

    private List<Case> buildCases(String policyName, LeavePolicyVO policy, boolean fiscalProrate) {
        List<Case> cases = new ArrayList<>();
        for (String[] d : GROUP_A) {
            for (String h : HANDLINGS) {
                cases.add(new Case(policyName, policy, fiscalProrate, "A", d[0], d[1], h));
            }
        }
        for (String[] d : GROUP_B) {
            for (String h : HANDLINGS) {
                cases.add(new Case(policyName, policy, fiscalProrate, "B", d[0], d[1], h));
            }
        }
        return cases;
    }

    // ============================ runner ============================

    @Test
    @DisplayName("prafta-029 full matrix (4 policies x dates x handlings) -> actual granted days")
    void runFullMatrix() {
        StringBuilder out = new StringBuilder();
        out.append("prafta-029 leave grant engine full-matrix actuals\n");
        out.append("TODAY=").append(TODAY).append("  fiscalStart=01-01  AXIS4=CEIL  validity=")
                .append(VALIDITY).append("mo  AXIS5=LEGAL(3/2/25)  freshUser(no prior grants)\n");

        runPolicy(out, "P1 HIRE_DATE/MONTHLY_ONLY", hirePolicy(), false);
        runPolicy(out, "P2 FISCAL/MONTHLY_ONLY", fiscalPolicy("MONTHLY_ONLY", "CEIL"), false);
        runPolicy(out, "P3 FISCAL/PRORATE(CEIL)", fiscalPolicy("PRORATE", "CEIL"), true);
        runPolicy(out, "P4 FISCAL/NEXT_YEAR_BULK", fiscalPolicy("NEXT_YEAR_BULK", "CEIL"), false);

        System.out.println(out);
        writeResultFile(out.toString());
    }

    private void writeResultFile(String content) {
        try {
            java.nio.file.Path p = java.nio.file.Paths.get("build", "prafta-029-results.txt");
            java.nio.file.Files.createDirectories(p.getParent());
            java.nio.file.Files.write(p, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("[prafta-029] results written to " + p.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("[prafta-029] failed to write result file: " + e.getMessage());
        }
    }

    private void runPolicy(StringBuilder out, String policyName, LeavePolicyVO policy, boolean fiscalProrate) {
        out.append('\n');
        out.append("================================================================\n");
        out.append("POLICY = ").append(policyName)
                .append("  | AXIS1=").append(policy.getAxis1GrantBase())
                .append(" AXIS3=").append(policy.getAxis3FirstYearMethod())
                .append(fiscalProrate ? " AXIS4=" + policy.getAxis4ProrateRounding() : "")
                .append('\n');
        out.append("cols: GROUP | LABEL | HIRE | HANDLING | TOTAL = MONTHLY(a) + BACKFILL(b) + CURRENT(c)")
                .append(fiscalProrate ? " | PRORATE raw->ceil (first partial period only)" : "")
                .append('\n');
        out.append("----------------------------------------------------------------\n");
        for (Case c : buildCases(policyName, policy, fiscalProrate)) {
            when(policySvc.findActivePolicy(anyString())).thenReturn(c.policy);
            when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn(c.hire);
            if (c.handling == null) {
                when(eng.selectLatestUnappliedHandling(eq(CMPNY), eq(USER))).thenReturn(null);
            } else {
                when(eng.selectLatestUnappliedHandling(eq(CMPNY), eq(USER)))
                        .thenReturn(Map.of("HANDLING_TYPE", c.handling, "HIST_ID", "H1"));
            }

            PolicyGrantPreviewVO r = svc.previewPolicyGrant(CMPNY, List.of(USER), MGR);
            PolicyGrantPreviewRowVO row = r.getRows().get(0);
            int total = row.getAddDays();
            assertTrue(total >= 0, () -> "addDays must be >= 0 for " + c.label);

            double[] bd = parseBreakdown(row.getNote());   // {backfill, monthly}
            double backfill = bd[0];
            double monthly = bd[1];
            double current = total - backfill - monthly;

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-2s | %-8s | %s | %-18s | TOTAL=%2d = a(%s) + b(%s) + c(%s)",
                    c.group, c.label, c.hire,
                    c.handling == null ? "APPLY_NEW(none)" : c.handling,
                    total, num(monthly), num(backfill), num(current)));

            if (fiscalProrate) {
                ProrateInfo pi = prorateRawIfFirstPartial(c.hire);
                if (pi != null) {
                    sb.append(String.format("  | PRORATE(crossed=1) raw=%.4f -> ceil=%s",
                            pi.raw, pi.ceil.stripTrailingZeros().toPlainString()));
                } else {
                    sb.append("  | PRORATE n/a (crossed=0 월차만 / crossed>=2 만연차15)");
                }
            }
            out.append(sb).append('\n');
        }
    }

    // ============================ handling-method comparison (existing grants) ============================

    /**
     * 처리방식(KEEP_AND_APPLY_NEW / KEEP_AND_BACKFILL / RESET_ALL) 비교 — 정방향 runner가 신규 사용자라
     * 셋이 동일하게 나오던 부분을, "원래 입사일로 이미 부여받은 직원"을 2단계로 모델링해 실제 차이를 드러낸다.
     *
     * <p>1단계: 원입사일 기준 fresh apply() → 그 직원이 현재 보유한 활성 STATUTORY 부여(멱등키·GRANT_ID·일수)를 캡처.
     * <p>2단계: 입사일을 변경값으로 바꾼 뒤, 캡처한 부여가 "이미 존재"하는 상태(countByIdempotencyKey/
     * selectActiveStatutoryGrantIds)로 처리방식별 apply()를 실행. 신규부여(INSERT)/취소건수/순활성잔액을 집계.
     */
    @Test
    @DisplayName("prafta-029 handling comparison (existing grants) -> APPLY_NEW vs BACKFILL vs RESET_ALL")
    void runHandlingComparison() {
        StringBuilder out = new StringBuilder();
        out.append("prafta-029 handling-method comparison (user already has grants from ORIGINAL hire date)\n");
        out.append("TODAY=").append(TODAY).append("  fiscalStart=01-01  validity=").append(VALIDITY)
                .append("mo  AXIS5=LEGAL  metric: granted(new INSERT) / canceled / netActive\n");
        out.append("netActive = existingDays - canceledDays + grantedDays\n");

        // 그룹B (안정적 1년초과): 과거변경은 3년차 진입(15->16)+라벨 이동, 미래변경은 라벨 우연 일치
        compareScenario(out, "P1 HIRE_DATE/MONTHLY_ONLY", hirePolicy(), "20230721", "20230211", "B 과거(2023-07-21->2023-02-11)");
        compareScenario(out, "P1 HIRE_DATE/MONTHLY_ONLY", hirePolicy(), "20230721", "20231211", "B 미래(2023-07-21->2023-12-11)");
        compareScenario(out, "P2 FISCAL/MONTHLY_ONLY", fiscalPolicy("MONTHLY_ONLY", "CEIL"), "20230721", "20230211", "B 과거");
        compareScenario(out, "P2 FISCAL/MONTHLY_ONLY", fiscalPolicy("MONTHLY_ONLY", "CEIL"), "20230721", "20231211", "B 미래");
        compareScenario(out, "P3 FISCAL/PRORATE(CEIL)", fiscalPolicy("PRORATE", "CEIL"), "20230721", "20230211", "B 과거");
        compareScenario(out, "P3 FISCAL/PRORATE(CEIL)", fiscalPolicy("PRORATE", "CEIL"), "20230721", "20231211", "B 미래");
        compareScenario(out, "P4 FISCAL/NEXT_YEAR_BULK", fiscalPolicy("NEXT_YEAR_BULK", "CEIL"), "20230721", "20230211", "B 과거");
        compareScenario(out, "P4 FISCAL/NEXT_YEAR_BULK", fiscalPolicy("NEXT_YEAR_BULK", "CEIL"), "20230721", "20231211", "B 미래");

        // 그룹A (원래 1년미만=월차만 보유) → 과거2로 변경 시 1년초과 전환
        compareScenario(out, "P1 HIRE_DATE/MONTHLY_ONLY", hirePolicy(), "20250721", "20250222", "A 과거2(2025-07-21->2025-02-22)");
        compareScenario(out, "P2 FISCAL/MONTHLY_ONLY", fiscalPolicy("MONTHLY_ONLY", "CEIL"), "20250721", "20250222", "A 과거2");

        System.out.println(out);
        writeFile("build/prafta-029-handling-comparison.txt", out.toString());
    }

    // ============================ 표준 모델 회귀 단정 ============================

    /**
     * 고용노동부 표준 모델 회귀 고정 (prafta-029 후속 수정). FISCAL/PRORATE는 crossed==1에 비례,
     * crossed==0은 월차만, crossed>=2는 만연차+근속. FISCAL/NEXT_YEAR_BULK는 crossed==1에 만연차 일괄.
     * 값이 다시 바뀌면(엔진 회귀) 즉시 실패하도록 명시 단정한다(신규사용자·기존부여0·validity 12mo 기준).
     */
    @Test
    @DisplayName("REGRESSION: 표준 모델 FISCAL 부여일수 고정 (PRORATE/NEXT_YEAR_BULK) — prafta-030 월차게이트 반영")
    void standardModelFiscalRegression() {
        // ⚠️ prafta-030 BE-2(D2) 월차 게이트는 "경력인정 고용승계 더블딥"(실근속<12 AND 산정근속>=12 AND full15)에만
        //    월차를 차단한다. 본 회귀는 모두 경력인정 0의 정상 근로자이므로 게이트 비대상 → 월차 보존(§8.5.4, 형평성 MODE B).
        //    따라서 종전 prafta-029 표준값(비례/만연차 + 법정 월차)을 그대로 유지한다(과잉게이트로 월차분을 뺐던 값 원복).
        LeavePolicyVO prorate = fiscalPolicy("PRORATE", "CEIL");
        // GROUP_A(1년미만, 정상근로자) crossed==1 → 비례 본연차 + 법정 월차(실근속 기준) 동시 부여
        assertEquals(17, previewDays(prorate, "20250721"), "PRORATE base 2025-07-21: 비례7 + 월차10 = 17");
        assertEquals(11, previewDays(prorate, "20251011"), "PRORATE future1 2025-10-11: 비례4 + 월차7 = 11");
        assertEquals(20, previewDays(prorate, "20250611"), "PRORATE past1 2025-06-11: 비례9 + 월차11 = 20");
        assertEquals(4, previewDays(prorate, "20260102"), "PRORATE future2 2026-01-02: crossed=0 본연차 미발생 → 월차만4");
        // prafta-030 D2-B: 만 1년 도래일(2025-02-22 + 1년 − 1일 = 2026-02-21)이 TODAY(2026-05-25) 이전이므로 첫해 월차 일괄 소멸 → 월차 0.
        assertEquals(13, previewDays(prorate, "20250222"), "PRORATE past2 2025-02-22: 비례13 + 월차0(만1년소멸) = 13");
        // GROUP_B (확정 1년초과, crossed>=2) → 만연차15 + 근속가산(3년차 +1)=16 (월차는 원래 소멸 → 0)
        assertEquals(16, previewDays(prorate, "20230721"), "PRORATE B base 2023-07-21: 만연차15 + 근속1");
        assertEquals(16, previewDays(prorate, "20230211"), "PRORATE B past 2023-02-11");

        LeavePolicyVO bulk = fiscalPolicy("NEXT_YEAR_BULK", "CEIL");
        // crossed==1 → 만연차 일괄(비례 아님) + 법정 월차(정상근로자, 게이트 비대상)
        assertEquals(25, previewDays(bulk, "20250721"), "BULK base 2025-07-21: 만연차15 + 월차10 = 25");
        assertEquals(4, previewDays(bulk, "20260102"), "BULK future2 2026-01-02: crossed=0 본연차 미발생 → 월차만4");
        assertEquals(16, previewDays(bulk, "20230721"), "BULK B base 2023-07-21: 만연차15 + 근속1");
    }

    /** 신규 사용자(기존 부여 0, 미적용 이력 없음) 기준 1명 부여 예정 일수. */
    private int previewDays(LeavePolicyVO policy, String hire) {
        when(policySvc.findActivePolicy(anyString())).thenReturn(policy);
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn(hire);
        when(eng.selectLatestUnappliedHandling(eq(CMPNY), eq(USER))).thenReturn(null);
        return svc.previewPolicyGrant(CMPNY, List.of(USER), MGR).getRows().get(0).getAddDays();
    }

    // ============================ prafta-029 옵션 A: CANCELED 재활성화 회귀 ============================

    /**
     * 옵션 A의 핵심 버그 수정 회귀. RESET_ALL 이 표준 월차키 {@code U001_202507_STATUTORY_MONTHLY} 를 CANCELED 로
     * 만든 뒤, KEEP_AND_BACKFILL 이 같은 기간을 다시 부여(reactivate)할 수 있어야 한다.
     *
     * <p>설정(2025-06-05 입사, TODAY=2026-05-25 → 11개월 완성, 월차 11일):
     * 7월(202507) 월차 표준키만 CANCELED(selectCanceledGrantIdByKey→GRANT_ID), live=0, suffix변형=0.
     * 그 외 키는 전부 fresh(INSERT). 기대: 202507 키는 reactivateCanceledGrant 1회, insertManualGrant 0회.
     */
    @Test
    @DisplayName("옵션 A: CANCELED 표준 월차키 202507 → KEEP_AND_BACKFILL 재부여 시 reactivate 1회 / insert 0회")
    void optionA_reactivatesCanceledStandardKey() {
        final String canceledKey = USER + "_202507_STATUTORY_MONTHLY";

        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy());
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20250605");
        when(eng.selectLatestUnappliedHandling(eq(CMPNY), eq(USER)))
                .thenReturn(Map.of("HANDLING_TYPE", "KEEP_AND_BACKFILL", "HIST_ID", "H1"));
        // live 기부여 없음(옵션 A: alreadyGranted 가 live-only) + 변형키 변형 없음 → 전부 신규/부활 대상
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countActiveBySuffixVariant(anyString(), anyString())).thenReturn(0);
        // 7월(202507) 표준 월차키만 CANCELED 단건 존재 → reactivate 대상. 그 외 키는 null(INSERT).
        when(dash.selectCanceledGrantIdByKey(eq(CMPNY), anyString())).thenReturn(null);
        when(dash.selectCanceledGrantIdByKey(eq(CMPNY), eq(canceledKey))).thenReturn("GCANCELED");
        when(dash.reactivateCanceledGrant(any(LeaveGrantInsertVO.class))).thenReturn(1);
        final AtomicInteger seq = new AtomicInteger();
        when(dash.selectNextGrantId(anyString())).thenAnswer(inv -> "G" + seq.incrementAndGet());

        HireDateGrantResultVO r = svc.hireDateGrant(CMPNY, List.of(USER), MGR, "OP");

        // 202507 키는 부활(UPDATE) 1회, 같은 키로 INSERT 는 0회
        ArgumentCaptor<LeaveGrantInsertVO> reCap = ArgumentCaptor.forClass(LeaveGrantInsertVO.class);
        verify(dash, times(1)).reactivateCanceledGrant(reCap.capture());
        assertEquals(canceledKey, reCap.getValue().getIdempotencyKey(), "부활 대상은 202507 표준 월차키");
        ArgumentCaptor<LeaveGrantInsertVO> insCap = ArgumentCaptor.forClass(LeaveGrantInsertVO.class);
        verify(dash, atLeast(0)).insertManualGrant(insCap.capture());
        for (LeaveGrantInsertVO vo : insCap.getAllValues()) {
            assertTrue(!canceledKey.equals(vo.getIdempotencyKey()),
                    () -> "CANCELED 표준키는 INSERT 가 아니라 reactivate 로만 부여돼야 함: " + vo.getIdempotencyKey());
        }
        // 부활된 1일이 부여 총일수에 반영됨(11개월 월차 모두 신규/부활 → 11일)
        assertEquals(0, r.getGrantedDays().compareTo(BigDecimal.valueOf(11)),
                () -> "월차 11일 부여(부활 1 + INSERT 10): grantedDays=" + r.getGrantedDays());
    }

    /**
     * 이중부여 방지(변형키 가드 유지). 같은 (기간·종류)에 RESET 회차키(_R) 등 ACTIVE 변형이 있으면
     * (countActiveBySuffixVariant>0) 표준키 클릭은 skip — reactivate/insert 둘 다 호출되지 않아야 한다.
     */
    @Test
    @DisplayName("옵션 A: 변형키(_R) ACTIVE 존재 시 표준키 클릭 skip → reactivate/insert 0회")
    void optionA_skipsWhenSuffixVariantActive() {
        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy());
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20250605");
        when(eng.selectLatestUnappliedHandling(eq(CMPNY), eq(USER))).thenReturn(null); // APPLY_NEW(표준키)
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        // 모든 기간에 ACTIVE 변형(_R 등) 존재 → 기부여로 간주, 표준키 재부여 차단
        when(dash.countActiveBySuffixVariant(anyString(), anyString())).thenReturn(1);
        when(dash.selectCanceledGrantIdByKey(eq(CMPNY), anyString())).thenReturn("GCANCELED");
        final AtomicInteger seq = new AtomicInteger();
        when(dash.selectNextGrantId(anyString())).thenAnswer(inv -> "G" + seq.incrementAndGet());

        HireDateGrantResultVO r = svc.hireDateGrant(CMPNY, List.of(USER), MGR, "OP");

        verify(dash, never()).reactivateCanceledGrant(any(LeaveGrantInsertVO.class));
        verify(dash, never()).insertManualGrant(any(LeaveGrantInsertVO.class));
        assertEquals(0, r.getGrantedCount(), "변형키 ACTIVE 존재 → 추가 부여 없음");
    }

    /**
     * EXHAUSTED/EXPIRED 등 live 기부여가 이미 있으면(countLiveByIdempotencyKey>0) 표준키는 skip.
     * (CANCELED 가 아닌 live 행은 옵션 A 부활 대상이 아니며 정상적으로 기부여로 인식돼야 한다.)
     */
    @Test
    @DisplayName("옵션 A: live 기부여 존재(EXHAUSTED/EXPIRED 등) → reactivate/insert 0회")
    void optionA_skipsWhenLiveGrantExists() {
        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy());
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20250605");
        when(eng.selectLatestUnappliedHandling(eq(CMPNY), eq(USER))).thenReturn(null);
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(1); // 전부 live 기부여
        when(dash.countActiveBySuffixVariant(anyString(), anyString())).thenReturn(0);
        when(dash.selectCanceledGrantIdByKey(eq(CMPNY), anyString())).thenReturn("GCANCELED");

        HireDateGrantResultVO r = svc.hireDateGrant(CMPNY, List.of(USER), MGR, "OP");

        verify(dash, never()).reactivateCanceledGrant(any(LeaveGrantInsertVO.class));
        verify(dash, never()).insertManualGrant(any(LeaveGrantInsertVO.class));
        assertEquals(0, r.getGrantedCount(), "live 기부여 존재 → 추가 부여 없음");
    }

    /**
     * preview 는 무쓰기. CANCELED 표준키가 있어 apply 라면 reactivate 할 상태라도, preview 는
     * addDays 에 재부여 예정분을 반영하되 reactivate/insert/cancel 을 단 한 번도 호출하지 않아야 한다.
     */
    @Test
    @DisplayName("옵션 A: preview 는 재부여 예정 반영 + reactivate/insert/cancel 0회(무쓰기)")
    void optionA_previewNoWrite() {
        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy());
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn("20250605");
        when(eng.selectLatestUnappliedHandling(eq(CMPNY), eq(USER)))
                .thenReturn(Map.of("HANDLING_TYPE", "KEEP_AND_BACKFILL", "HIST_ID", "H1"));
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countActiveBySuffixVariant(anyString(), anyString())).thenReturn(0);
        when(dash.selectCanceledGrantIdByKey(eq(CMPNY), endsWith("_202507_STATUTORY_MONTHLY")))
                .thenReturn("GCANCELED");

        PolicyGrantPreviewVO r = svc.previewPolicyGrant(CMPNY, List.of(USER), MGR);

        // 무쓰기 보장: reactivate/insert/cancel 0회
        verify(dash, never()).reactivateCanceledGrant(any(LeaveGrantInsertVO.class));
        verify(dash, never()).insertManualGrant(any(LeaveGrantInsertVO.class));
        verify(statusSvc, never()).cancelGrant(anyString(), anyString(), anyString());
        // 재부여 예정분(월차 11일 포함)이 addDays 에 반영됨
        assertEquals(11, r.getRows().get(0).getAddDays(),
                () -> "preview addDays 에 월차 11일 재부여 예정 반영: note=" + r.getRows().get(0).getNote());
    }

    private void compareScenario(StringBuilder out, String policyName, LeavePolicyVO policy,
                                 String originalHire, String newHire, String label) {
        // ---- 1단계: 원입사일 기준 기존 활성 부여 캡처 (fresh, APPLY_NEW) ----
        resetFresh();
        when(policySvc.findActivePolicy(anyString())).thenReturn(policy);
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn(originalHire);
        svc.hireDateGrant(CMPNY, List.of(USER), MGR, "OP");

        ArgumentCaptor<LeaveGrantInsertVO> cap = ArgumentCaptor.forClass(LeaveGrantInsertVO.class);
        verify(dash, atLeast(0)).insertManualGrant(cap.capture());
        Set<String> existingKeys = new HashSet<>();
        List<String> existingGrantIds = new ArrayList<>();
        BigDecimal existingDays = BigDecimal.ZERO;
        for (LeaveGrantInsertVO vo : cap.getAllValues()) {
            existingKeys.add(vo.getIdempotencyKey());
            existingGrantIds.add(vo.getGrantId());
            existingDays = existingDays.add(vo.getGrantDays());
        }

        out.append('\n').append("---- ").append(policyName).append(" | ").append(label)
                .append("  | 기존부여(원입사일)=").append(plain(existingDays)).append("일 ")
                .append(existingGrantIds.size()).append("건 ----\n");

        // ---- 2단계: 변경 입사일 + 처리방식별 apply (기존 부여가 존재하는 상태) ----
        for (String h : new String[] {"KEEP_AND_APPLY_NEW", "KEEP_AND_BACKFILL", "RESET_ALL"}) {
            resetFresh();
            when(policySvc.findActivePolicy(anyString())).thenReturn(policy);
            when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn(newHire);
            when(eng.selectLatestUnappliedHandling(eq(CMPNY), eq(USER)))
                    .thenReturn(Map.of("HANDLING_TYPE", h, "HIST_ID", "H1"));
            final Set<String> ek = existingKeys;
            when(dash.countByIdempotencyKey(anyString(), anyString()))
                    .thenAnswer(inv -> ek.contains((String) inv.getArgument(1)) ? 1 : 0);
            // 이 시나리오의 기존 부여는 ACTIVE(=live)다 → live-only 판정에도 동일하게 1로 인식되도록 미러 스텁.
            when(dash.countLiveByIdempotencyKey(anyString(), anyString()))
                    .thenAnswer(inv -> ek.contains((String) inv.getArgument(1)) ? 1 : 0);
            when(eng.selectActiveStatutoryGrantIds(eq(CMPNY), eq(USER)))
                    .thenReturn(new ArrayList<>(existingGrantIds));

            HireDateGrantResultVO r = svc.hireDateGrant(CMPNY, List.of(USER), MGR, "OP");
            BigDecimal granted = (r.getGrantedDays() == null) ? BigDecimal.ZERO : r.getGrantedDays();
            int canceled = r.getCanceledCount();
            // RESET_ALL은 기존 활성 STATUTORY 전부 취소 → 취소일수 = 기존부여일수, 그 외는 0
            BigDecimal canceledDays = "RESET_ALL".equals(h) ? existingDays : BigDecimal.ZERO;
            BigDecimal net = existingDays.subtract(canceledDays).add(granted);
            out.append(String.format("  %-19s | 신규부여=%-5s 취소=%d건(%s일) | 순활성잔액=%s일%n",
                    h, plain(granted), canceled, plain(canceledDays), plain(net)));
            assertTrue(net.signum() >= 0, () -> "net >= 0 for " + h);
        }
    }

    private void resetFresh() {
        reset(dash, eng, policySvc, statusSvc);
        when(dash.countActiveUser(anyString(), anyString())).thenReturn(1);
        when(dash.countLeaveTypeExists(anyString(), anyString())).thenReturn(1);
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(0);
        when(dash.countByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countActiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(eng.selectLatestUnappliedHandling(anyString(), anyString())).thenReturn(null);
        when(eng.selectActiveStatutoryGrantIds(anyString(), anyString())).thenReturn(new ArrayList<>());
        final AtomicInteger seq = new AtomicInteger();
        when(dash.selectNextGrantId(anyString())).thenAnswer(inv -> "G" + seq.incrementAndGet());
    }

    private String plain(BigDecimal d) {
        return d.stripTrailingZeros().toPlainString();
    }

    private void writeFile(String relPath, String content) {
        try {
            java.nio.file.Path p = java.nio.file.Paths.get(relPath);
            if (p.getParent() != null) {
                java.nio.file.Files.createDirectories(p.getParent());
            }
            java.nio.file.Files.write(p, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("[prafta-029] written to " + p.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("[prafta-029] failed to write " + relPath + ": " + e.getMessage());
        }
    }

    // ============================ fiscal first-year timing demo ============================

    /**
     * 회계연도 기준(P2 MONTHLY_ONLY) 신규 입사자에게 "법정 본연차 15일"이 실제로 언제부터 부여되는지를,
     * 입사일 2025-07-21을 고정하고 "현재일(TODAY)"만 바꿔가며 실측한다. 엔진은 입사 후 회계연도 시작
     * (01-01)을 1회라도 넘기면(crossedFiscalStarts>=1) 만연차 15를 부여한다 → 근속 1년 미만 시점인
     * 2026-01-01부터 15일이 켜지는지 확인한다(사용자 질의 검증).
     */
    @Test
    @DisplayName("DEMO: FISCAL/MONTHLY_ONLY 신규입사자의 15일 부여 개시 시점 (입사 2025-07-21)")
    void demoFiscalFirstYearTiming() {
        StringBuilder out = new StringBuilder();
        out.append("FISCAL/MONTHLY_ONLY(01-01) 입사 2025-07-21 — 현재일별 부여 (신규사용자, validity 12mo)\n");
        out.append("cols: TODAY | 근속개월 | crossedFiscalStarts | TOTAL = a(월차)+b+c(본연차+근속)\n");
        out.append("----------------------------------------------------------------\n");

        String hire = "20250721";
        LeavePolicyVO policy = fiscalPolicy("MONTHLY_ONLY", "CEIL");
        LocalDate hireD = LocalDate.parse(hire, DateTimeFormatter.BASIC_ISO_DATE);

        LocalDate[] days = {
                LocalDate.of(2025, 9, 15),
                LocalDate.of(2025, 12, 31),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 7, 21),
                LocalDate.of(2027, 1, 1),
        };
        for (LocalDate d : days) {
            localDateMock.when(LocalDate::now).thenReturn(d);
            when(policySvc.findActivePolicy(anyString())).thenReturn(policy);
            when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn(hire);
            when(eng.selectLatestUnappliedHandling(eq(CMPNY), eq(USER))).thenReturn(null);

            PolicyGrantPreviewVO r = svc.previewPolicyGrant(CMPNY, List.of(USER), MGR);
            PolicyGrantPreviewRowVO row = r.getRows().get(0);
            int total = row.getAddDays();
            double[] bd = parseBreakdown(row.getNote());
            double monthly = bd[1];
            double current = total - bd[0] - monthly;

            int months = (int) ChronoUnit.MONTHS.between(hireD, d);
            int crossed = 0;
            for (int y = hireD.getYear(); y <= d.getYear(); y++) {
                LocalDate fs = LocalDate.of(y, 1, 1);
                if (!fs.isBefore(hireD) && !fs.isAfter(d)) {
                    crossed++;
                }
            }
            out.append(String.format("%s | 근속 %2d개월 | crossed=%d | TOTAL=%2d = a(%s)+b(%s)+c(%s)%s%n",
                    d, months, crossed, total, num(monthly), num(bd[0]), num(current),
                    current > 0 ? "   <== 본연차 15 개시" : ""));
        }
        localDateMock.when(LocalDate::now).thenReturn(TODAY); // 원복
        System.out.println(out);
        writeFile("build/prafta-029-fiscal-timing.txt", out.toString());
    }

    // ============================ helpers ============================

    /**
     * Parse the engine's preview note. When backfill or monthly &gt; 0 the note has the shape
     * "<added> N <unit>(<backfill-label> X / <monthly-label> Y)" — the only note that contains '/'.
     * Numbers in order are [N(total), X(backfill), Y(monthly)]. Returns {backfill, monthly}.
     * For other notes (null / "already granted" / "no target") returns {0,0} (total is all current).
     */
    private double[] parseBreakdown(String note) {
        if (note != null && note.contains("/")) {
            Matcher m = Pattern.compile("[0-9]+(?:\\.[0-9]+)?").matcher(note);
            List<Double> nums = new ArrayList<>();
            while (m.find()) {
                nums.add(Double.parseDouble(m.group()));
            }
            if (nums.size() >= 3) {
                return new double[] {nums.get(1), nums.get(2)};
            }
        }
        return new double[] {0, 0};
    }

    private static final class ProrateInfo {
        final double raw;
        final BigDecimal ceil;
        ProrateInfo(double raw, BigDecimal ceil) {
            this.raw = raw;
            this.ceil = ceil;
        }
    }

    /**
     * 표준 모델: PRORATE 비례 본연차는 <b>crossed==1</b>(입사 후 처음 도래한 회계연도 시작)에서만 적용된다.
     * 비례 base = (currentFiscalStart − hire) 일수(입사가 속한 첫 부분기) ÷ 365 × 15. CEIL은 엔진의
     * package-private {@code computeProratedAnnualDays}로 산출(엔진과 동일 산식 검증). crossed==0(월차만)·
     * crossed&gt;=2(만연차 15)에서는 비례가 적용되지 않으므로 null.
     */
    private ProrateInfo prorateRawIfFirstPartial(String hireYmd) {
        LocalDate hire = LocalDate.parse(hireYmd, DateTimeFormatter.BASIC_ISO_DATE);
        // fiscal start 01-01: current fiscal start = Jan 1 of TODAY's year
        LocalDate currentFiscalStart = LocalDate.of(TODAY.getYear(), 1, 1);
        // crossed = number of Jan-1 in (hire, today]
        int crossed = 0;
        for (int y = hire.getYear(); y <= TODAY.getYear(); y++) {
            LocalDate fs = LocalDate.of(y, 1, 1);
            if (!fs.isBefore(hire) && !fs.isAfter(TODAY)) {
                crossed++;
            }
        }
        if (crossed != 1) {
            return null; // crossed==0: 월차만(본연차 0) / crossed>=2: 만연차 15
        }
        // 표준 모델 비례 base = 입사가 속한 첫 부분기(입사일 ~ 도래한 회계연도 시작)
        long days = ChronoUnit.DAYS.between(hire, currentFiscalStart);
        if (days < 0) {
            days = 0;
        }
        if (days > 365) {
            days = 365;
        }
        double raw = (days / 365.0) * BASE_ANNUAL;
        BigDecimal ceil = svc.computeProratedAnnualDays(hire, currentFiscalStart, "CEIL");
        return new ProrateInfo(raw, ceil);
    }

    private String num(double d) {
        if (d == Math.floor(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }
}
