package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.mapper.LeaveGrantEngineMapper;
import com.prafta.common.cmm.leave.service.LeaveGrantStatusService;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.cmm.leave.vo.PolicyGrantPreviewVO;
import com.prafta.common.schedule.leave.scheduler.LeaveGrantScheduler;

/**
 * prafta-023 연차 부여 엔진 런타임 시나리오 검증 (prafta-026, 순수 로직 + Mockito).
 *
 * <p>{@code prafta-023-verification-checklist.md}의 런타임 항목(A/C/#2/D·#3/#1/F/E)을 결정적으로 검증한다.
 * 매퍼·정책 서비스는 Mockito mock 으로 대체하고, {@code LocalDate.now()}는 {@link #TODAY}로 고정해
 * 날짜 기반 분기를 재현 가능하게 만든다(운영 코드 변경 없음 — 정적 메서드 mock).
 *
 * <p>대상은 USER_CD 기준. 기대값은 TODAY=2026-05-15 기준으로 산정한다.
 */
class LeaveGrantEngineScenarioTest {

    /** 고정 "오늘" — 월 중순(15일)이라 말일/경계 보정 영향 없음. */
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 15);
    private static final String CMPNY = "C001";
    private static final String USER = "U001";
    private static final String MGR = "master";
    private static final String OP = "OP001";

    private LeaveDashboardMapper dash;
    private LeaveGrantEngineMapper eng;
    private LeavePolicyService policySvc;
    private LeaveGrantStatusService statusSvc;
    private LeaveGrantEngineServiceImpl svc;
    private MockedStatic<LocalDate> localDateMock;

    @BeforeEach
    void setUp() {
        // LocalDate.now() 만 고정, of/parse 등은 실제 동작 유지.
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

        // 공통 기본 stub (미사용 stub 은 manual mock 이라 strict 검사 없음)
        when(dash.countActiveUser(anyString(), anyString())).thenReturn(1);
        when(dash.countLeaveTypeExists(anyString(), anyString())).thenReturn(1);
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(0);
        when(dash.countByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        // prafta-029 옵션 A: alreadyGranted 가 live-only(countLiveByIdempotencyKey) 로 전환됨 → 기부여 판정 스텁도 함께.
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countActiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countActiveBySuffixVariant(anyString(), anyString())).thenReturn(0);
        when(dash.selectNextGrantId(anyString())).thenReturn("G1", "G2", "G3", "G4", "G5", "G6");
        when(eng.selectLatestUnappliedHandling(anyString(), anyString())).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        localDateMock.close();
    }

    // ====================== 헬퍼 ======================

    private LeavePolicyVO hirePolicy(int validityMonths) {
        LeavePolicyVO p = new LeavePolicyVO();
        p.setPolicySeq(1L);
        p.setAxis1GrantBase("HIRE_DATE");
        p.setAxis6ValidityMonths(validityMonths);
        return p;
    }

    private LeavePolicyVO fiscalPolicy(String startMm, String startDd, String firstYearMethod,
                                       String rounding, int validityMonths) {
        LeavePolicyVO p = new LeavePolicyVO();
        p.setPolicySeq(1L);
        p.setAxis1GrantBase("FISCAL_YEAR");
        p.setAxis2FiscalStartMm(startMm);
        p.setAxis2FiscalStartDd(startDd);
        p.setAxis3FirstYearMethod(firstYearMethod);
        p.setAxis4ProrateRounding(rounding);
        p.setAxis6ValidityMonths(validityMonths);
        return p;
    }

    private void givenPolicy(LeavePolicyVO p) {
        when(policySvc.findActivePolicy(anyString())).thenReturn(p);
    }

    private void givenHire(String hireYmd) {
        when(dash.selectUserHireDate(eq(CMPNY), eq(USER))).thenReturn(hireYmd);
    }

    private PolicyGrantPreviewVO preview() {
        return svc.previewPolicyGrant(CMPNY, List.of(USER), MGR);
    }

    // ====================== A. 멱등키 dual-read ======================

    @Test
    @DisplayName("A: 동일 키로 이미 부여됨 → 추가 부여 0, 변경없음")
    void a_idempotent_allGranted() {
        givenPolicy(hirePolicy(24));
        givenHire("20230110"); // 3년 초과 근속 → 정상이면 당기 본연차+근속가산
        // 전부 기존 live 부여(ACTIVE) → live-only 판정에도 1. (옵션 A에서 alreadyGranted 가 countLive 를 봄)
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(1);

        PolicyGrantPreviewVO r = preview();

        assertEquals(0, r.getRows().get(0).getAddDays());
        assertEquals(1, r.getNoChangeCount());
        assertTrue(r.getRows().get(0).getNote().contains("이미 부여됨"),
                () -> "note=" + r.getRows().get(0).getNote());
    }

    @Test
    @DisplayName("A: 레거시 _HIRE 키만 존재해도 dual-read 로 중복 인식 → 추가 부여 0")
    void a_idempotent_legacyHireKey() {
        givenPolicy(hirePolicy(24));
        givenHire("20230110");
        // 정식 키는 없고 레거시 _HIRE 키만 live 존재 (옵션 A: alreadyGranted 가 countLive 로 dual-read)
        when(dash.countLiveByIdempotencyKey(anyString(), anyString()))
                .thenAnswer(inv -> ((String) inv.getArgument(1)).contains("_HIRE") ? 1 : 0);

        PolicyGrantPreviewVO r = preview();

        assertEquals(0, r.getRows().get(0).getAddDays(), "레거시 _HIRE 키를 인식해 이중부여를 막아야 함");
    }

    // ====================== C. APPLY_NEW (HIRE_DATE) ======================
    // (prafta-032 009: @Disabled c_backfill_vs_applyNew 물리 삭제 — 처리방식 BACKFILL 소급 부여 분기가
    //  부여 엔진에서 제거되어 검증 대상이 사라짐. estimate 차액 검증은 c_estimate_validityExclusion에 보존.)

    @Test
    @DisplayName("C/F(prafta-030): estimateBackfillDays = 차액(새 기준 누적 − 기존 누적). 기존을 당기분으로 고정하면 소급분만 남음")
    void c_estimate_validityExclusion() {
        givenHire("20230110"); // 2023-01-10 입사 (3년차, 당기 본연차15+가산1=16)
        // prafta-030(정정): estimateBackfillDays 가 차액 산식으로 정합. 기존 부여누적(전 STATUTORY, 소멸제외·사용포함)을
        //   당기분 16으로 고정하면 차액 = (당기16 + 유효 소급분) − 16 = 유효 소급분만 남아 종전 의미와 동일.
        when(dash.selectStatutoryGrantAccrual(eq(CMPNY), eq(USER), anyString()))
                .thenReturn(java.math.BigDecimal.valueOf(16));

        givenPolicy(hirePolicy(24));
        // 2024분(유효 24개월=2026-01까지, 오늘 2026-05 기준 만료) 제외, 2025분(2027-01까지 유효) 15일만 소급
        assertEquals(15, svc.estimateBackfillDays(CMPNY, USER, "20230110"));

        givenPolicy(hirePolicy(12));
        // 유효 12개월 → 2024/2025분 모두 소멸, 2026분은 당기 담당 → 소급 0 (차액 = 16 − 16 = 0)
        assertEquals(0, svc.estimateBackfillDays(CMPNY, USER, "20230110"));
    }

    @Test
    @DisplayName("C 라벨버그(prafta-028): 11월 입사 → 당기 라벨=최근 기념일연도(2025), 달력연도(2026)와 갈려 중복부여 안 함")
    void c_labelBug_novemberHire_noDuplicate() {
        // prafta-032 D1 정정 — 이 테스트는 폐기된 backfill 동작이 아니라 "당기부여 라벨 통일(prafta-028 라벨버그)"을 본다.
        //   라벨버그 가드(buildUserPlan의 달력연도 dual-read 전환가드)는 폐기 대상이 아니라 살아있는 동작이다.
        //   다만 종전 단정값 31은 폐기된 backfill(당기16 + 소급15)에 의존했으므로, 처리방식 무관 단일 KEEP(신규부여만)에
        //   맞춰 당기분만으로 재작성한다(green-washing 아님 — 아래 근거로 16이 옳다).
        givenPolicy(hirePolicy(24));
        givenHire("20221115"); // 2022-11-15 입사: 올해(2026) 기념일(11월) 미도래 → 최근 기념일은 작년(2025-11)

        PolicyGrantPreviewVO r = preview();

        // 근거: TODAY=2026-05-15, 입사 2022-11-15 → creditedMonths=42 → 3년차.
        //   당기부여 = 본연차15 + 근속가산(year3, AXIS5 3/2/25 → tenureBonus(3)=1) = 16, 라벨 = 최근 기념일연도 "2025".
        //   처리방식 폐기로 소급(backfill) 경로 미진입 → 당기분 16만. (종전 31 = 16 + 폐기된 백필 15)
        //   라벨버그 가드 검증 포인트: 당기 라벨이 달력연도 "2026"이 아니라 기념일연도 "2025"여야,
        //   향후 동일 근속연차가 '달력연도' 키와 '기념일연도' 키로 갈라져 이중부여되는 일이 없다.
        assertEquals(16, r.getRows().get(0).getAddDays(),
                "당기부여(최근 기념일연도 라벨)만 — 처리방식 폐기로 소급 없음, 본연차15+근속가산1=16");
    }

    // ====================== #2. FISCAL_YEAR 과거 백필 ======================

    @Test
    @DisplayName("#2(prafta-030): FISCAL_YEAR 과거 회계연도 소급(당해 제외, 유효기간 내). 차액 산식으로 백필 반영")
    void fiscal_backfill() {
        givenPolicy(fiscalPolicy("01", "01", "MONTHLY_ONLY", "CEIL", 24));

        // prafta-030(정정): estimateBackfillDays = 차액. 기존 부여누적을 당기분으로 고정하면 차액이 유효 소급분과 같다.
        // 2024-03-10 입사: 당기 본연차 15, 2025 회계연도(2025-01-01)만 유효(2027-01까지) → 소급 15 (차액 = 30 − 15)
        when(dash.selectStatutoryGrantAccrual(eq(CMPNY), eq(USER), anyString()))
                .thenReturn(java.math.BigDecimal.valueOf(15));
        assertEquals(15, svc.estimateBackfillDays(CMPNY, USER, "20240310"));

        // 2022-03-10 입사: 당기 본연차15+가산1=16, 2025분(근속 3년차 가산 +1)=16 유효, 2023/2024 소멸, 2026 당해 제외 → 소급 16
        when(dash.selectStatutoryGrantAccrual(eq(CMPNY), eq(USER), anyString()))
                .thenReturn(java.math.BigDecimal.valueOf(16));
        assertEquals(16, svc.estimateBackfillDays(CMPNY, USER, "20220310"));
    }

    // ====================== D/#3. PRORATE 비례부여 ======================

    @Test
    @DisplayName("D/#3(prafta-029 표준모델): crossed==0 첫 부분기간 → 본연차 미부여(월차만). PRORATE/NEXT_YEAR_BULK 동일")
    void prorate_firstPartialPeriod_crossed0_monthlyOnly() {
        // 2026-05-01 입사(근속 0개월 → 월차 없음), 회계연도 01-01, 첫 부분기간(crossedFiscalStarts==0).
        // 표준 모델: crossed==0이면 본연차 없음(월차만). PRORATE도 NEXT_YEAR_BULK와 동일하게 0.
        givenHire("20260501");

        givenPolicy(fiscalPolicy("01", "01", "PRORATE", "CEIL", 12));
        PolicyGrantPreviewVO prorated = preview();
        assertEquals(0, prorated.getRows().get(0).getAddDays(),
                "crossed==0 PRORATE는 본연차 미부여(월차만), 근속 0개월이라 총 0");

        givenPolicy(fiscalPolicy("01", "01", "NEXT_YEAR_BULK", "CEIL", 12));
        PolicyGrantPreviewVO bulk = preview();
        assertEquals(0, bulk.getRows().get(0).getAddDays(),
                "crossed==0 NEXT_YEAR_BULK도 본연차 미부여(월차만), 총 0");
    }

    @Test
    @DisplayName("D/#3(prafta-030 월차게이트 정정): crossed==1 정상 근로자(경력인정0) → 월차 보존. PRORATE 비례7+월차9 / NEXT_YEAR_BULK 15+월차9")
    void prorate_crossed1_proratedVsBulk() {
        // 2025-07-21 입사, 회계연도 01-01, TODAY=2026-05-15 → 2026-01-01 1회 도래(crossedFiscalStarts==1).
        // 부분기 = DAYS.between(2025-07-21, 2026-01-01) = 164일 → 15*164/365=6.74 → CEIL 7.
        // 월차 = 실근속 9개월(2025-07-21~2026-05-15) → 9일.
        // ⚠️ prafta-030 BE-2(D2 정정 2026-05-26): 월차 게이트는 "고용승계 더블딥"에만 적용 — 경력인정 없는
        //    정상 근로자는 월차 보존. PRORATE 비례<15라 더블딥 아님(차단 안 함). 정상 비례 중도입사자 월차 보존.
        givenHire("20250721");

        givenPolicy(fiscalPolicy("01", "01", "PRORATE", "CEIL", 12));
        PolicyGrantPreviewVO prorated = preview();
        assertEquals(16, prorated.getRows().get(0).getAddDays(), "비례 본연차 7 + 월차 9 = 16(정상 근로자 월차 보존)");

        // NEXT_YEAR_BULK: crossed==1 이면 본연차 15 일괄 + 월차 9 = 24.
        givenPolicy(fiscalPolicy("01", "01", "NEXT_YEAR_BULK", "CEIL", 12));
        PolicyGrantPreviewVO bulk = preview();
        assertEquals(24, bulk.getRows().get(0).getAddDays(), "NEXT_YEAR_BULK 본연차 15 + 월차 9 = 24(정상 근로자 월차 보존)");
    }

    // ====================== #1. 월차 per-월 누적 ======================

    @Test
    @DisplayName("#1: 1년 미만 신규직원 → 완성 개월수만큼 월차 누적(per-월)")
    void monthly_accrual_newEmployee() {
        givenPolicy(hirePolicy(12));
        givenHire("20251215"); // 2025-12-15 입사 → 오늘까지 5개월 완성

        PolicyGrantPreviewVO r = preview();
        assertEquals(5, r.getRows().get(0).getAddDays(), "5개월 → 월차 5일");
        assertTrue(r.getRows().get(0).getNote().contains("월차 5"),
                () -> "note=" + r.getRows().get(0).getNote());
    }

    @Test
    @DisplayName("#1: 레거시 ACTIVE 집계 월차 보유 연도는 per-월 상호배타로 건너뜀")
    void monthly_mutualExclusion() {
        givenPolicy(hirePolicy(12));
        givenHire("20251215");
        // 해당 연도에 ACTIVE 집계 월차 존재 → per-월 건너뜀
        when(dash.countActiveByIdempotencyKey(anyString(), anyString())).thenReturn(1);

        PolicyGrantPreviewVO r = preview();
        assertEquals(0, r.getRows().get(0).getAddDays(), "집계 월차 보유 연도는 per-월 미부여");
    }

    // ====================== prafta-029. RESET 회차키 ↔ 표준키 누수 차단 ======================

    @Test
    @DisplayName("prafta-029: 표준키 클릭 — 회차키(_R) ACTIVE 변형이 있으면 같은 기간 재부여 안 함")
    void standardClick_skipsWhenResetRoundActive() {
        givenPolicy(hirePolicy(12));
        givenHire("20251215"); // 1년 미만 → 월차만(전환가드 없으면 5일 부여)
        // 같은 (기간·종류)에 RESET_ALL 회차키(_R)로 이미 ACTIVE 부여가 있다고 가정 (표준키 자기 자신은 0)
        when(dash.countActiveBySuffixVariant(anyString(), anyString())).thenReturn(1);

        PolicyGrantPreviewVO r = preview();

        // 표준키 클릭(APPLY_NEW)은 회차키로 이미 ACTIVE 부여된 기간을 재부여하면 안 됨 → 추가 0.
        assertEquals(0, r.getRows().get(0).getAddDays(),
                "회차키(_R)로 이미 ACTIVE 부여된 기간을 표준키 클릭이 재부여하면 안 됨");
    }

    // ====================== E. 자동 정기부여 배치 게이트 ======================
    // (prafta-032 009: @Disabled resetAll_cancelsThenReissues 물리 삭제 — RESET_ALL 취소+재발급 분기가
    //  부여 엔진에서 제거되어 검증 대상이 사라짐.)

    @Test
    @DisplayName("E: 게이트 기본 비활성 → 엔진 미호출. 활성 시 1회 호출")
    void autoGrant_gate() {
        LeaveGrantScheduler scheduler = new LeaveGrantScheduler(svc);

        // 기본(false): 엔진 미호출
        scheduler.runAutoGrant();
        // 활성 정책 회사 없음 → 엔진 진입해도 부여 0 (게이트 OFF 라 진입 자체 없음)
        verify(eng, never()).selectAutoGrantCompanyCds();

        // 게이트 ON: 엔진 1회 진입
        ReflectionTestUtils.setField(scheduler, "autoGrantEnabled", true);
        when(eng.selectAutoGrantCompanyCds()).thenReturn(List.of()); // 대상 회사 없음 → 0명
        scheduler.runAutoGrant();
        verify(eng, times(1)).selectAutoGrantCompanyCds();
    }

    @Test
    @DisplayName("E: runScheduledAutoGrant 대상 회사 0곳 → 0명 부여")
    void autoGrant_noCompanies() {
        when(eng.selectAutoGrantCompanyCds()).thenReturn(List.of());
        assertEquals(0, svc.runScheduledAutoGrant());
    }

    // ====================== 기본 동작 sanity ======================

    @Test
    @DisplayName("sanity: preview 가 정상 행을 반환(권한/스코프 가드 통과)")
    void preview_sanity() {
        givenPolicy(hirePolicy(24));
        givenHire("20240115");
        PolicyGrantPreviewVO r = preview();
        assertNotNull(r);
        assertEquals(1, r.getSelectedCount());
        assertEquals(1, r.getRows().size());
    }
}
