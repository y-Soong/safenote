package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.command.ManualGrantCommand;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.leave.vo.LeaveGrantInsertVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.cmm.leave.vo.LeaveTypeAvailTermVO;
import com.prafta.common.exception.ApiException;

/**
 * prafta-045 — 관리자 수동 부여 AVAIL_TO_DATE 산출 분기 단위테스트.
 *
 * <p>타입의 사용가능기간(SYS026 ADMIN_AVAIL_TERM_TYPE)에 따라 부여건 AVAIL_TO_DATE 가
 * 01(무기한 sentinel)/02(해당연도1231)/03(타입 to)/null(AXIS6 폴백)로 산출되는지를,
 * {@code insertManualGrant} 로 흘러가는 {@link LeaveGrantInsertVO} 를 캡처해 단정한다.
 *
 * <p>매퍼/정책 서비스는 Mockito mock, "오늘"은 {@code mockStatic(LocalDate)} 로 고정한다.
 * AVAIL_FROM_DATE 는 폼 입력을 그대로 유지함을 함께 확인한다(§3-3). 법정 엔진은 본 경로와 무관하다.
 */
class LeaveDashboardManualAvailTermTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 4);
    private static final String CMPNY = "C001";
    private static final String AUTH_MASTER = "master";
    private static final String OPERATOR = "OP001";
    private static final String TARGET_USER = "U001";
    private static final String LEAVE_CD = "L999";
    private static final String FORM_AVAIL_FROM = "20260601";

    private LeaveDashboardMapper dash;
    private LeavePolicyService policy;
    private LeaveGrantEngineService engine;
    private LeaveDashboardServiceImpl svc;
    private MockedStatic<LocalDate> localDateMock;

    @BeforeEach
    void setUp() {
        dash = mock(LeaveDashboardMapper.class);
        policy = mock(LeavePolicyService.class);
        engine = mock(LeaveGrantEngineService.class);
        svc = new LeaveDashboardServiceImpl(dash, policy, engine, new ObjectMapper());

        // "오늘" 고정 (idempotency key / GRANT_DATE 산출 결정화)
        localDateMock = mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS);
        localDateMock.when(LocalDate::now).thenReturn(TODAY);

        // 공통 스텁: 권한 가드는 authCd=AUTH_MASTER 로 통과(AuthRoleUtils.isManager). 화이트리스트/활성사용자/채번 통과.
        lenient().when(dash.countManualGrantType(eq(CMPNY), eq(LEAVE_CD))).thenReturn(1);
        lenient().when(dash.countActiveUser(eq(CMPNY), eq(TARGET_USER))).thenReturn(1);
        lenient().when(dash.selectNextGrantId(eq(CMPNY))).thenReturn("G202606040001");
        lenient().when(dash.insertManualGrant(any(LeaveGrantInsertVO.class))).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        localDateMock.close();
    }

    private ManualGrantCommand command() {
        return new ManualGrantCommand(List.of(TARGET_USER), LEAVE_CD,
                BigDecimal.valueOf(3), FORM_AVAIL_FROM, "사유");
    }

    private void stubAvailTerm(String termType, String from, String to) {
        LeaveTypeAvailTermVO vo = new LeaveTypeAvailTermVO();
        vo.setAdminAvailTermType(termType);
        vo.setAdminAvailFromDt(from);
        vo.setAdminAvailToDt(to);
        when(dash.selectAdminAvailTerm(eq(CMPNY), eq(LEAVE_CD))).thenReturn(vo);
    }

    /** insertManualGrant 로 흘러간 VO 를 캡처한다. */
    private LeaveGrantInsertVO captureInserted() {
        ArgumentCaptor<LeaveGrantInsertVO> cap = ArgumentCaptor.forClass(LeaveGrantInsertVO.class);
        verify(dash).insertManualGrant(cap.capture());
        return cap.getValue();
    }

    @Test
    @DisplayName("01 설정안함(무기한) → AVAIL_TO_DATE = 99991231 sentinel, AVAIL_FROM = 폼 입력 유지")
    void availTerm01_forever() {
        stubAvailTerm("01", null, null);

        svc.manualGrant(CMPNY, command(), AUTH_MASTER, OPERATOR);

        LeaveGrantInsertVO vo = captureInserted();
        assertEquals("99991231", vo.getAvailToDate());
        assertEquals(FORM_AVAIL_FROM, vo.getAvailFromDate());
        // 무기한은 AXIS6(정책) 조회를 타지 않는다.
        verify(policy, never()).findActivePolicy(anyString());
    }

    @Test
    @DisplayName("02 해당 연도 내 → AVAIL_TO_DATE = (폼 사용가능일 연도)1231")
    void availTerm02_currentYear() {
        stubAvailTerm("02", null, null);

        svc.manualGrant(CMPNY, command(), AUTH_MASTER, OPERATOR);

        LeaveGrantInsertVO vo = captureInserted();
        // 폼 availFromDate=20260601 → 2026 + 1231
        assertEquals("20261231", vo.getAvailToDate());
        assertEquals(FORM_AVAIL_FROM, vo.getAvailFromDate());
    }

    @Test
    @DisplayName("03 기간 설정 → AVAIL_TO_DATE = 타입 ADMIN_AVAIL_TO_DT(YYYYMMDD 절대일)")
    void availTerm03_period() {
        stubAvailTerm("03", "20260101", "20271130");

        svc.manualGrant(CMPNY, command(), AUTH_MASTER, OPERATOR);

        LeaveGrantInsertVO vo = captureInserted();
        assertEquals("20271130", vo.getAvailToDate());
        // from 은 폼 입력 유지(타입 from 으로 덮지 않음).
        assertEquals(FORM_AVAIL_FROM, vo.getAvailFromDate());
    }

    @Test
    @DisplayName("null 미설정 → AXIS6 폴백 (폼 from + 활성정책 validityMonths)")
    void availTermNull_axis6Fallback() {
        stubAvailTerm(null, null, null);
        // 활성 정책 유효 24개월
        LeavePolicyVO p = mock(LeavePolicyVO.class);
        when(p.getAxis6ValidityMonths()).thenReturn(24);
        when(policy.findActivePolicy(eq(CMPNY))).thenReturn(p);

        svc.manualGrant(CMPNY, command(), AUTH_MASTER, OPERATOR);

        LeaveGrantInsertVO vo = captureInserted();
        // 20260601 + 24개월 = 20280601
        assertEquals("20280601", vo.getAvailToDate());
        assertEquals(FORM_AVAIL_FROM, vo.getAvailFromDate());
    }

    @Test
    @DisplayName("타입 avail-term 조회 자체가 null → AXIS6 폴백(정책 없음 → 기본 12개월)")
    void availTermVoNull_axis6FallbackDefault() {
        when(dash.selectAdminAvailTerm(eq(CMPNY), eq(LEAVE_CD))).thenReturn(null);
        when(policy.findActivePolicy(eq(CMPNY))).thenReturn(null);

        svc.manualGrant(CMPNY, command(), AUTH_MASTER, OPERATOR);

        LeaveGrantInsertVO vo = captureInserted();
        // 정책 없음 → 기본 12개월. 20260601 + 12개월 = 20270601
        assertEquals("20270601", vo.getAvailToDate());
    }

    @Test
    @DisplayName("03 기간설정인데 ADMIN_AVAIL_TO_DT 부적합(미설정) → AXIS6 폴백")
    void availTerm03_invalidTo_fallback() {
        stubAvailTerm("03", "20260101", null);
        when(policy.findActivePolicy(eq(CMPNY))).thenReturn(null);

        svc.manualGrant(CMPNY, command(), AUTH_MASTER, OPERATOR);

        LeaveGrantInsertVO vo = captureInserted();
        // 폴백 기본 12개월
        assertEquals("20270601", vo.getAvailToDate());
    }

    @Test
    @DisplayName("from > to 모순(03 타입 to 가 폼 from 이전) → ATTD_400_032 거부, INSERT 미실행")
    void availTerm03_fromAfterTo_rejected() {
        // 폼 from=20260601, 타입 to=20260101 → from > to
        stubAvailTerm("03", "20250101", "20260101");

        assertThrows(ApiException.class,
                () -> svc.manualGrant(CMPNY, command(), AUTH_MASTER, OPERATOR));

        verify(dash, never()).insertManualGrant(any(LeaveGrantInsertVO.class));
    }
}
