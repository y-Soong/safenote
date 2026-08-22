package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.cmm.leave.vo.ShortfallCandidateVO;
import com.prafta.common.cmm.leave.vo.ShortfallListResultVO;
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 경력인정 이원화 Phase 2 §2-2 — 입사일 기준 차액 조회({@code getShortfallList}) 단위테스트.
 *
 * <p>AXIS1=FISCAL_YEAR 게이트(탭 비노출 판정), ensureManager 게이트(P-13: master/hr 전용 —
 * safe·부서 관리자 403), 남은 부족분 산식(정답 누적 − P-12 live 법정 부여 총량, 기보전 이중차감
 * 금지)을 검증한다.
 */
class LeaveDashboardShortfallListTest {

    private static final String CMPNY = "C001";
    private static final String AUTH_MASTER = "master";
    private static final String GV_USER = "ADMIN1";
    private static final String GV_SITE = "S001";
    private static final String BASE_YMD = "20260821";

    private LeaveDashboardMapper dash;
    private LeavePolicyService policy;
    private LeaveGrantEngineService engine;
    private LeaveDashboardServiceImpl svc;

    @BeforeEach
    void setUp() {
        dash = mock(LeaveDashboardMapper.class);
        policy = mock(LeavePolicyService.class);
        engine = mock(LeaveGrantEngineService.class);
        SiteAccessService siteAccessService = mock(SiteAccessService.class);
        svc = new LeaveDashboardServiceImpl(dash, policy, engine, new ObjectMapper(),
                mock(LeaveConversionPolicyService.class), siteAccessService);
    }

    private LeavePolicyVO fiscalPolicy() {
        LeavePolicyVO p = new LeavePolicyVO();
        p.setAxis1GrantBase("FISCAL_YEAR");
        return p;
    }

    private LeavePolicyVO hirePolicy() {
        LeavePolicyVO p = new LeavePolicyVO();
        p.setAxis1GrantBase("HIRE_DATE");
        return p;
    }

    @Test
    @DisplayName("HIRE_DATE 회사 — fiscalYearYn=N, rows 빈 배열(에러 아님, 탭 비노출 판정용), 후보 조회 미실행")
    void hireDateCompany_returnsMetaOnly() {
        when(policy.findActivePolicy(eq(CMPNY))).thenReturn(hirePolicy());

        ShortfallListResultVO result = svc.getShortfallList(
                CMPNY, AUTH_MASTER, GV_USER, GV_SITE, "", "", "N", "", BASE_YMD, 1, 20);

        assertEquals("N", result.getFiscalYearYn());
        assertTrue(result.getRows().isEmpty());
        verify(dash, never()).selectShortfallCandidateList(anyString(), any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("FISCAL_YEAR 회사 — 남은 부족분 = 정답누적 - 실제부여누적(기보전 이중차감 없음)")
    void fiscalCompany_computesRemainingShortfall() {
        when(policy.findActivePolicy(eq(CMPNY))).thenReturn(fiscalPolicy());

        ShortfallCandidateVO cand = new ShortfallCandidateVO();
        cand.setUserCd("U001");
        cand.setUserNm("홍길동");
        cand.setHireDate("20200101");

        when(dash.countShortfallCandidateList(eq(CMPNY), any(), any(), any(), any())).thenReturn(1L);
        when(dash.selectShortfallCandidateList(eq(CMPNY), any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of(cand));
        when(engine.computeHireBasisAccrual(eq(CMPNY), eq("U001"), any(LocalDate.class)))
                .thenReturn(BigDecimal.valueOf(10));
        // P-12: 실제 부여 누적 = live 법정 부여 총량(사용·만료 무관 — 기준일 파라미터 없음)
        when(dash.selectStatutoryGrantedLiveTotal(eq(CMPNY), eq("U001")))
                .thenReturn(BigDecimal.valueOf(6));
        when(dash.selectCoverGrantTotal(eq(CMPNY), eq("U001"))).thenReturn(BigDecimal.valueOf(2));

        ShortfallListResultVO result = svc.getShortfallList(
                CMPNY, AUTH_MASTER, GV_USER, GV_SITE, "", "", "N", "", BASE_YMD, 1, 20);

        assertEquals("Y", result.getFiscalYearYn());
        assertEquals(1, result.getRows().size());
        var row = result.getRows().get(0);
        assertEquals(0, BigDecimal.valueOf(10).compareTo(row.getHireBasisAccrual()));
        assertEquals(0, BigDecimal.valueOf(6).compareTo(row.getActualAccrual()));
        // 6 안에 기보전(_COVER) 2일이 이미 포함돼 있다는 전제(BE-1 정의) — 남은부족분은 다시 빼지 않는다.
        assertEquals(0, BigDecimal.valueOf(4).compareTo(row.getDiff()));
        assertEquals(0, BigDecimal.valueOf(4).compareTo(row.getRemainingShortfall()));
        assertEquals(0, BigDecimal.valueOf(2).compareTo(row.getCoveredTotal()));

        // ★P-12 축 전환 고정: 종전 축(selectStatutoryGrantAccrual, 소멸 제외+사용 포함)은 호출되지
        //   않아야 한다 — 그 축이면 소멸 월차 미사용분이 빠져 부족분이 사용량에 따라 흔들린다(P2R2-N1).
        verify(dash, never()).selectStatutoryGrantAccrual(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("★P-12(QTUSERB 재현): 정답 26 · 소멸 월차 11 부여 — 사용량(0/5.5/11)과 무관하게 "
            + "live 총량 축은 SUM(GRANT_DAYS)=11 로 동일 → 남은 부족분 15.0 수렴(종전 26.0/20.5/15.0 요동 해소)")
    void p12_shortfallInvariantToUsageAndExpiry() {
        when(policy.findActivePolicy(eq(CMPNY))).thenReturn(fiscalPolicy());

        ShortfallCandidateVO cand = new ShortfallCandidateVO();
        cand.setUserCd("QTUSERB");
        cand.setUserNm("큐티유저비");
        cand.setHireDate("20250712");

        when(dash.countShortfallCandidateList(eq(CMPNY), any(), any(), any(), any())).thenReturn(1L);
        when(dash.selectShortfallCandidateList(eq(CMPNY), any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of(cand));
        when(engine.computeHireBasisAccrual(eq(CMPNY), eq("QTUSERB"), any(LocalDate.class)))
                .thenReturn(new BigDecimal("26.0"));
        // 월차 11일 부여·소멸(AVAIL_TO 과거, STATUS=EXPIRED 전이 포함) 상태 — 사용량이 0이든 5.5든 11이든
        //   SUM(GRANT_DAYS) 는 11.0 하나다(사용·만료는 SQL 축에서 원천 배제 → 서비스 결과도 단일 값).
        when(dash.selectStatutoryGrantedLiveTotal(eq(CMPNY), eq("QTUSERB")))
                .thenReturn(new BigDecimal("11.0"));
        when(dash.selectCoverGrantTotal(eq(CMPNY), eq("QTUSERB"))).thenReturn(BigDecimal.ZERO);

        ShortfallListResultVO result = svc.getShortfallList(
                CMPNY, AUTH_MASTER, GV_USER, GV_SITE, "", "", "N", "", BASE_YMD, 1, 20);

        var row = result.getRows().get(0);
        assertEquals(0, new BigDecimal("15.0").compareTo(row.getRemainingShortfall()),
                "노무상 정답 15.0 — 소멸 월차가 부족분으로 부활(26.0)하거나 사용량 의존(20.5)하면 안 된다");
        verify(dash, never()).selectStatutoryGrantAccrual(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("★P-13(SEC-P2-2): 차액 조회는 master/hr 전용 — safe(산업안전관리자) ATTD_403_020 "
            + "(종전 canManageNode 채택으로 열리던 연차 도메인 최초 safe 개방 회수)")
    void p13_safeManagerDenied() {
        ApiException ex = assertThrows(ApiException.class, () -> svc.getShortfallList(
                CMPNY, "safe", GV_USER, GV_SITE, "", "", "N", "", BASE_YMD, 1, 20));
        assertEquals(AttdErrorCode.ATTD_403_020, ex.getErrorCode());
        verify(dash, never()).countShortfallCandidateList(anyString(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("★P-13: 일반 사원 — ATTD_403_020 (Attd_09 본문 ensureManager 와 동일 게이트)")
    void p13_plainUserDenied() {
        ApiException ex = assertThrows(ApiException.class, () -> svc.getShortfallList(
                CMPNY, "user", GV_USER, GV_SITE, "", "", "N", "", BASE_YMD, 1, 20));
        assertEquals(AttdErrorCode.ATTD_403_020, ex.getErrorCode());
    }

    @Test
    @DisplayName("기준일 형식 불량 — COMMON_400_001")
    void invalidBaseYmd_rejected() {
        ApiException ex = assertThrows(ApiException.class, () -> svc.getShortfallList(
                CMPNY, AUTH_MASTER, GV_USER, GV_SITE, "", "", "N", "", "2026-08-21", 1, 20));
        assertEquals(CommonErrorCode.COMMON_400_001, ex.getErrorCode());
    }
}
