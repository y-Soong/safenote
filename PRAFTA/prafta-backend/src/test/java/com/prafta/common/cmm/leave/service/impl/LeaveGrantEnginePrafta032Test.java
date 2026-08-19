package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.mapper.LeaveGrantEngineMapper;
import com.prafta.common.cmm.leave.service.LeaveGrantStatusService;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.leave.vo.HireDateAdjustResultVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantInsertVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantRecallRowVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.exception.ApiException;

/**
 * prafta-032 — 입사일 변경 "수동 연차 조정"(추가/회수) 결정적 단위테스트.
 *
 * <p>단일 출처: {@code .claude/requests/prafta-032-decisions.md}(D1·D3·D4·D5).
 * 처리방식 자동계산(KEEP 계열·RESET_ALL)을 폐기하고, 관리자가 입력한 목표 법정 부여량과 현재값의
 * 차액을 추가(D4)/회수(D5)하는 명시 경로({@code adjustStatutoryGrantsByHireDateChange})를 검증한다.
 *
 * <p>{@code mockStatic(LocalDate)}로 오늘=2026-05-26 고정. 매퍼/정책 서비스는 Mockito mock.
 * 추가 부여 GRANT_DATE 정합(과제 #3): 기존 정책 기준 부여(grantComponent)와 동일하게 GRANT_DATE=오늘,
 * 발생일은 AVAIL_FROM_DATE에 담는다. 따라서 소급 부여행도 GRANT_DATE=오늘 / AVAIL_FROM=발생일을 단정한다.
 */
class LeaveGrantEnginePrafta032Test {

    /** 고정 "오늘" = prafta-030 테스트와 동일 기준일(월말 보정 영향 없는 26일). */
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 26);
    private static final String TODAY_YMD = "20260526";
    private static final String CMPNY = "C001";
    private static final String USER = "U001";
    private static final String OP = "OP001";
    private static final String HIST = "H1";

    private static final String GRANT_TYPE_ANNUAL = "STATUTORY_ANNUAL";
    private static final String GRANT_TYPE_MONTHLY = "STATUTORY_MONTHLY";
    private static final String GRANT_TYPE_TENURE = "STATUTORY_TENURE_BONUS";
    private static final String BACKFILL_REASON_CODE = "INSADAY_CHANGE_BACKFILL";
    private static final String OVERAGE_REASON_CODE = "MANUAL_OVERAGE";

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

        // 공통 기본 stub — 멱등 판정은 전부 0(미부여)이라 산정 발생일이 newInsert=true로 잡힌다.
        when(dash.selectCreditMonths(anyString(), anyString())).thenReturn(0);
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countActiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        when(dash.countActiveBySuffixVariant(anyString(), anyString())).thenReturn(0);
        when(policySvc.findActivePolicy(anyString())).thenReturn(hirePolicy(12));
        final AtomicInteger seq = new AtomicInteger();
        when(dash.selectNextGrantId(anyString())).thenAnswer(inv -> "G" + seq.incrementAndGet());
    }

    @AfterEach
    void tearDown() {
        localDateMock.close();
    }

    // ============================ policy builder ============================

    private LeavePolicyVO hirePolicy(int validityMonths) {
        LeavePolicyVO p = new LeavePolicyVO();
        p.setPolicySeq(1L);
        p.setAxis1GrantBase("HIRE_DATE");
        p.setAxis6ValidityMonths(validityMonths);
        return p;
    }

    /** 차액 기준선(현재 법정 부여량)과 회수가능량 stub. */
    private void givenCurrentAndRecallable(BigDecimal currentTotal, BigDecimal recallable) {
        when(eng.selectActiveStatutoryGrantedTotal(eq(CMPNY), eq(USER))).thenReturn(currentTotal);
        when(eng.selectRecallableStatutoryTotal(eq(CMPNY), eq(USER))).thenReturn(recallable);
    }

    /** insertManualGrant로 들어온 모든 INSERT를 순서대로 캡처. */
    private List<LeaveGrantInsertVO> capturedInserts() {
        ArgumentCaptor<LeaveGrantInsertVO> cap = ArgumentCaptor.forClass(LeaveGrantInsertVO.class);
        verify(dash, atLeast(0)).insertManualGrant(cap.capture());
        return cap.getAllValues();
    }

    private static LeaveGrantRecallRowVO recallRow(String grantId, String grantType, double grantDays,
                                                   double usedDays, String availTo) {
        LeaveGrantRecallRowVO v = new LeaveGrantRecallRowVO();
        v.setGrantId(grantId);
        v.setGrantType(grantType);
        v.setGrantDays(BigDecimal.valueOf(grantDays));
        v.setUsedDays(BigDecimal.valueOf(usedDays));
        v.setAvailToDate(availTo);
        return v;
    }

    private static int days(BigDecimal d) {
        return d == null ? 0 : d.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
    }

    // ============================ target=null / 음수 ============================

    @Test
    @DisplayName("adjust_targetNull_noop: target=null → 추가0·회수0·스냅샷 무처리(INSERT/cancel/reduce 미호출)")
    void adjust_targetNull_noop() {
        givenCurrentAndRecallable(BigDecimal.valueOf(15), BigDecimal.valueOf(15));

        HireDateAdjustResultVO r = svc.adjustStatutoryGrantsByHireDateChange(
                CMPNY, USER, "20240101", null, null, HIST, OP);

        assertEquals(0, days(r.getAddedDays()), "추가 0");
        assertEquals(0, days(r.getWithdrawnDays()), "회수 0");
        assertEquals(0, r.getAddedGrantCount(), "추가 건수 0");
        assertEquals(0, r.getCanceledGrantCount(), "취소 건수 0");
        assertEquals(0, r.getReducedGrantCount(), "차감 건수 0");
        assertNull(r.getAffectedSnapshotJson(), "스냅샷 없음");
        verify(dash, never()).insertManualGrant(any());
        verify(eng, never()).cancelStatutoryGrantForHireChange(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(eng, never()).reduceStatutoryGrantDaysForHireChange(anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("adjust_negativeTarget: target<0 → USER_400_032 예외(INSERT/cancel/reduce 미호출)")
    void adjust_negativeTarget() {
        givenCurrentAndRecallable(BigDecimal.valueOf(10), BigDecimal.valueOf(10));

        assertThrows(ApiException.class, () -> svc.adjustStatutoryGrantsByHireDateChange(
                CMPNY, USER, "20240101", BigDecimal.valueOf(-1), null, HIST, OP));

        verify(dash, never()).insertManualGrant(any());
        verify(eng, never()).cancelStatutoryGrantForHireChange(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(eng, never()).reduceStatutoryGrantDaysForHireChange(anyString(), anyString(), any(), anyString(), anyString());
    }

    // ============================ 추가 부여 (차액>0) ============================

    @Test
    @DisplayName("adjust_add_backfillOnly(TC-201): 과거이동·발생일 충분 → 소급 부여만(GRANT_DATE=오늘/AVAIL_FROM=발생일, _HD키, '01', BACKFILL)")
    void adjust_add_backfillOnly() {
        // 새 입사일 2024-01-01, TODAY=2026-05-26 → 유효 소급 발생일 1건(2026-01-01 본연차 15, availTo 2027-01-01).
        //   (2025-01-01분은 유효기간 12개월 → availTo 2026-01-01 < today 라 소멸 제외. 월차는 첫해 만1년 경과로 전부 제외)
        // 현재 법정 10, 목표 15 → 차액 +5. 발생일 1건(15일) ≥ 차액 5 → 전부 소급(폴백 없음).
        givenCurrentAndRecallable(BigDecimal.valueOf(10), BigDecimal.valueOf(10));

        HireDateAdjustResultVO r = svc.adjustStatutoryGrantsByHireDateChange(
                CMPNY, USER, "20240101", BigDecimal.valueOf(15), null, HIST, OP);

        assertEquals(5, days(r.getAddedDays()), "추가 = 차액 5");
        assertEquals(1, r.getAddedGrantCount(), "소급 1건만(폴백 없음)");
        assertEquals(0, r.getWithdrawnDays() == null ? 0 : r.getWithdrawnDays().intValue(), "회수 0");

        List<LeaveGrantInsertVO> inserts = capturedInserts();
        assertEquals(1, inserts.size(), "INSERT 1건");
        LeaveGrantInsertVO bf = inserts.get(0);
        assertEquals(5, days(bf.getGrantDays()), "소급 부여 5일(발생일 일수 15 중 차액 5만)");
        assertEquals("01", bf.getGrantByType(), "GRANT_BY_TYPE='01'(정책 기반 산정)");
        assertTrue(bf.getGrantReason() != null && bf.getGrantReason().contains(BACKFILL_REASON_CODE),
                () -> "소급 사유 INSADAY_CHANGE_BACKFILL: " + bf.getGrantReason());
        assertTrue(bf.getIdempotencyKey() != null && bf.getIdempotencyKey().contains("_HD" + HIST),
                () -> "멱등키 _HD{histId} 네임스페이스: " + bf.getIdempotencyKey());
        // GRANT_DATE 정합(과제 #3): 기존 부여 컨벤션과 동일하게 GRANT_DATE=오늘, 발생일은 AVAIL_FROM에 담는다.
        assertEquals(TODAY_YMD, bf.getGrantDate(), "GRANT_DATE=오늘(기존 grantComponent 컨벤션과 일관)");
        assertEquals("20260101", bf.getAvailFromDate(), "AVAIL_FROM=미부여 발생일(2026-01-01)");
        assertEquals(GRANT_TYPE_ANNUAL, bf.getGrantType(), "발생일 시점 본연차");
    }

    @Test
    @DisplayName("adjust_add_todayFallback(TC-202/203): 발생일<추가일수 → 마지막 INSERT는 MANUAL_OVERAGE·GRANT_DATE=오늘·AVAIL_FROM=오늘")
    void adjust_add_todayFallback() {
        // 동일 새 입사일(2024-01-01): 유효 소급 발생일 1건(15일). 현재 0, 목표 20 → 차액 +20.
        //   소급 15 소진 후 잔여 5는 오늘 폴백. 폴백 GRANT_TYPE = 오늘 시점 산정근속(28개월) → ANNUAL.
        givenCurrentAndRecallable(BigDecimal.ZERO, BigDecimal.ZERO);

        HireDateAdjustResultVO r = svc.adjustStatutoryGrantsByHireDateChange(
                CMPNY, USER, "20240101", BigDecimal.valueOf(20), null, HIST, OP);

        assertEquals(20, days(r.getAddedDays()), "추가 = 차액 20(소급15 + 폴백5)");
        assertEquals(2, r.getAddedGrantCount(), "소급 1건 + 폴백 1건");

        List<LeaveGrantInsertVO> inserts = capturedInserts();
        assertEquals(2, inserts.size(), "INSERT 2건");
        LeaveGrantInsertVO bf = inserts.get(0);
        LeaveGrantInsertVO overage = inserts.get(inserts.size() - 1);

        // 1) 소급분: 발생일 일수 15 전부, AVAIL_FROM=발생일
        assertEquals(15, days(bf.getGrantDays()), "소급 15(발생일 일수 전부)");
        assertTrue(bf.getGrantReason().contains(BACKFILL_REASON_CODE), () -> "소급 사유: " + bf.getGrantReason());
        assertEquals("20260101", bf.getAvailFromDate(), "소급 AVAIL_FROM=발생일");
        // 2) 오늘 폴백분: 잔여 5, MANUAL_OVERAGE, GRANT_DATE=오늘, AVAIL_FROM=오늘
        assertEquals(5, days(overage.getGrantDays()), "폴백 잔여 5");
        assertTrue(overage.getGrantReason() != null && overage.getGrantReason().contains(OVERAGE_REASON_CODE),
                () -> "폴백 사유 MANUAL_OVERAGE: " + overage.getGrantReason());
        assertEquals(TODAY_YMD, overage.getGrantDate(), "폴백 GRANT_DATE=오늘");
        assertEquals(TODAY_YMD, overage.getAvailFromDate(), "폴백 AVAIL_FROM=오늘");
        assertEquals("01", overage.getGrantByType(), "폴백 GRANT_BY_TYPE='01'");
    }

    // ============================ resolveGrantTypeAt 경계 (TC-204) ============================

    @Test
    @DisplayName("resolveGrantTypeAt_boundaries(TC-204): creditedMonths 11→MONTHLY, 12→ANNUAL, 35→ANNUAL, 36→TENURE_BONUS")
    void resolveGrantTypeAt_boundaries() {
        // creditMonths=0 전제(setUp). hire 고정 후 atYmd로 경과개월을 정확히 11/12/35/36에 맞춘다.
        when(dash.selectCreditMonths(eq(CMPNY), eq(USER))).thenReturn(0);

        assertEquals(GRANT_TYPE_MONTHLY,
                ReflectionTestUtils.invokeMethod(svc, "resolveGrantTypeAt", CMPNY, USER, "20230101", "20231201"),
                "11개월 → MONTHLY");
        assertEquals(GRANT_TYPE_ANNUAL,
                ReflectionTestUtils.invokeMethod(svc, "resolveGrantTypeAt", CMPNY, USER, "20230101", "20240101"),
                "12개월 → ANNUAL");
        assertEquals(GRANT_TYPE_ANNUAL,
                ReflectionTestUtils.invokeMethod(svc, "resolveGrantTypeAt", CMPNY, USER, "20230101", "20251201"),
                "35개월 → ANNUAL");
        assertEquals(GRANT_TYPE_TENURE,
                ReflectionTestUtils.invokeMethod(svc, "resolveGrantTypeAt", CMPNY, USER, "20230101", "20260101"),
                "36개월 → TENURE_BONUS");
    }

    // ============================ 회수 (차액<0) ============================

    @Test
    @DisplayName("adjust_recall_block(TC-102): recall=3 > recallable=2 → 예외(회수 초과), cancel/reduce/insert 미호출(롤백)")
    void adjust_recall_block() {
        // 현재 10, 목표 7 → 회수 시도 3 > 회수가능 2 → 차단.
        givenCurrentAndRecallable(BigDecimal.valueOf(10), BigDecimal.valueOf(2));

        assertThrows(ApiException.class, () -> svc.adjustStatutoryGrantsByHireDateChange(
                CMPNY, USER, "20240101", BigDecimal.valueOf(7), "회수 사유", HIST, OP));

        verify(eng, never()).selectActiveStatutoryGrantsForRecall(anyString(), anyString());
        verify(eng, never()).cancelStatutoryGrantForHireChange(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(eng, never()).reduceStatutoryGrantDaysForHireChange(anyString(), anyString(), any(), anyString(), anyString());
        verify(dash, never()).insertManualGrant(any());
    }

    @Test
    @DisplayName("adjust_recall_reasonRequired(TC-103): diff<0 + 회수사유 공백 → 예외(USER_400_031)")
    void adjust_recall_reasonRequired() {
        // 현재 10, 목표 8 → 회수 발생. 회수가능 충분(10)이지만 회수 사유 공백 → 차단(사유 검증이 회수가능 검증보다 먼저).
        givenCurrentAndRecallable(BigDecimal.valueOf(10), BigDecimal.valueOf(10));

        assertThrows(ApiException.class, () -> svc.adjustStatutoryGrantsByHireDateChange(
                CMPNY, USER, "20240101", BigDecimal.valueOf(8), "   ", HIST, OP));

        verify(eng, never()).cancelStatutoryGrantForHireChange(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(eng, never()).reduceStatutoryGrantDaysForHireChange(anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("adjust_recall_fullCancel(TC-301): grantDays=2/used=0, recall=2 → cancel 1회(차감 0)")
    void adjust_recall_fullCancel() {
        // 현재 2, 목표 0 → 회수 2. 잔여 전체(2)·used=0 → STATUS='CANCELED'.
        givenCurrentAndRecallable(BigDecimal.valueOf(2), BigDecimal.valueOf(2));
        when(eng.selectActiveStatutoryGrantsForRecall(eq(CMPNY), eq(USER)))
                .thenReturn(List.of(recallRow("GA", GRANT_TYPE_ANNUAL, 2, 0, "20270101")));
        when(eng.cancelStatutoryGrantForHireChange(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1);

        HireDateAdjustResultVO r = svc.adjustStatutoryGrantsByHireDateChange(
                CMPNY, USER, "20240101", BigDecimal.ZERO, "회수 사유", HIST, OP);

        assertEquals(1, r.getCanceledGrantCount(), "취소 1건");
        assertEquals(0, r.getReducedGrantCount(), "차감 0건");
        assertEquals(2, days(r.getWithdrawnDays()), "회수 2일");
        verify(eng, times(1)).cancelStatutoryGrantForHireChange(eq(CMPNY), eq("GA"), anyString(), anyString(), eq(OP));
        verify(eng, never()).reduceStatutoryGrantDaysForHireChange(anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("adjust_recall_partialReduce(TC-302): grantDays=5/used=0, recall=2 → reduce(2) 1회(취소 0)")
    void adjust_recall_partialReduce() {
        // 현재 5, 목표 3 → 회수 2. 행 잔여 5 중 부분 회수 2 → GRANT_DAYS 직접 차감.
        givenCurrentAndRecallable(BigDecimal.valueOf(5), BigDecimal.valueOf(5));
        when(eng.selectActiveStatutoryGrantsForRecall(eq(CMPNY), eq(USER)))
                .thenReturn(List.of(recallRow("GB", GRANT_TYPE_ANNUAL, 5, 0, "20270101")));
        when(eng.reduceStatutoryGrantDaysForHireChange(anyString(), anyString(), any(), anyString(), anyString()))
                .thenReturn(1);

        HireDateAdjustResultVO r = svc.adjustStatutoryGrantsByHireDateChange(
                CMPNY, USER, "20240101", BigDecimal.valueOf(3), "회수 사유", HIST, OP);

        assertEquals(0, r.getCanceledGrantCount(), "취소 0건");
        assertEquals(1, r.getReducedGrantCount(), "차감 1건");
        assertEquals(2, days(r.getWithdrawnDays()), "회수 2일");

        ArgumentCaptor<BigDecimal> reduceCap = ArgumentCaptor.forClass(BigDecimal.class);
        verify(eng, times(1)).reduceStatutoryGrantDaysForHireChange(
                eq(CMPNY), eq("GB"), reduceCap.capture(), anyString(), eq(OP));
        assertEquals(2, days(reduceCap.getValue()), "차감량 2일");
        verify(eng, never()).cancelStatutoryGrantForHireChange(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("adjust_recall_usedPreserved(경계): grantDays=5/used=3(잔여2), recall=2 → reduce(2), used 불변(취소 0)")
    void adjust_recall_usedPreserved() {
        // 현재 5, 목표 3 → 회수 2. 행 잔여 = 5-3 = 2 전체 회수이나 used>0 → CANCELED 금지, GRANT_DAYS 차감만.
        //   used(3)는 엔진/SQL 모두 불변 — 서비스 레벨에선 cancel 대신 reduce가 호출되는지로 보존을 검증한다.
        givenCurrentAndRecallable(BigDecimal.valueOf(5), BigDecimal.valueOf(2));
        when(eng.selectActiveStatutoryGrantsForRecall(eq(CMPNY), eq(USER)))
                .thenReturn(List.of(recallRow("GC", GRANT_TYPE_ANNUAL, 5, 3, "20270101")));
        when(eng.reduceStatutoryGrantDaysForHireChange(anyString(), anyString(), any(), anyString(), anyString()))
                .thenReturn(1);

        HireDateAdjustResultVO r = svc.adjustStatutoryGrantsByHireDateChange(
                CMPNY, USER, "20240101", BigDecimal.valueOf(3), "회수 사유", HIST, OP);

        assertEquals(0, r.getCanceledGrantCount(), "used>0 → 취소 금지(사용분 보존)");
        assertEquals(1, r.getReducedGrantCount(), "GRANT_DAYS 차감 1건");
        assertEquals(2, days(r.getWithdrawnDays()), "회수 2일(행 잔여 전체)");

        ArgumentCaptor<BigDecimal> reduceCap = ArgumentCaptor.forClass(BigDecimal.class);
        verify(eng, times(1)).reduceStatutoryGrantDaysForHireChange(
                eq(CMPNY), eq("GC"), reduceCap.capture(), anyString(), eq(OP));
        assertEquals(2, days(reduceCap.getValue()), "차감량 2일(used는 SQL에서 불변)");
        // CANCELED 전환은 USED_DAYS=0 행만 대상이므로 used>0 행에는 cancel이 호출되면 안 됨.
        verify(eng, never()).cancelStatutoryGrantForHireChange(anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
