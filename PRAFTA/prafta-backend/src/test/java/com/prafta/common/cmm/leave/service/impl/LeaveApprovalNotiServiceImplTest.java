package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveApprovalNotiMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.service.LeaveApprovalNotiConst;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;

/**
 * PRAFTA-COM-004 — 연차 결재 PUSH 생산자(outbox 적재) 단위테스트.
 *
 * <p>매퍼는 Mockito mock. 메시지 빌더(종일/반차/시간차) 본문 포맷, 시나리오 A/B 적재,
 * 멱등(DuplicateKey 흡수), 예외 격리, 자기 알림 제외를 검증한다.
 */
class LeaveApprovalNotiServiceImplTest {

    private static final String CMPNY = "C001";
    private static final String SITE = "S001";
    private static final String APPLICANT = "U001";
    private static final String APPLICANT_NM = "홍길동";
    private static final String REQ_ID = "R20260603001";
    private static final String LEAVE_ID = "L20260603001";

    private LeaveApprovalNotiMapper notiMapper;
    private LeaveDashboardMapper dashboardMapper;
    private LeaveApprovalNotiServiceImpl service;

    @BeforeEach
    void setUp() {
        notiMapper = org.mockito.Mockito.mock(LeaveApprovalNotiMapper.class);
        dashboardMapper = org.mockito.Mockito.mock(LeaveDashboardMapper.class);
        service = new LeaveApprovalNotiServiceImpl(notiMapper, dashboardMapper, new ObjectMapper());
        // 기본 stub: 신청자명 평문 + NOTI_ID 채번.
        when(notiMapper.selectUserNm(CMPNY, APPLICANT)).thenReturn(APPLICANT_NM);
        when(dashboardMapper.selectNextNotiId(CMPNY)).thenReturn("N1", "N2", "N3");
    }

    // ───────────────────────── 시나리오 A ─────────────────────────

    @Test
    @DisplayName("시나리오 A: 차례 도래 결재자 1인에게 outbox 1건(notiType/dedup/body 확인)")
    void approvalTurn_insertsOneOutbox() {
        service.notifyApprovalTurn(CMPNY, SITE, APPLICANT, REQ_ID, 2, "MGR1", APPLICANT);

        ArgumentCaptor<NotiOutboxInsertVO> cap = ArgumentCaptor.forClass(NotiOutboxInsertVO.class);
        verify(dashboardMapper, times(1)).insertNotiOutbox(cap.capture());
        NotiOutboxInsertVO o = cap.getValue();
        assertEquals("MGR1", o.getTargetUserCd());
        assertEquals(LeaveApprovalNotiConst.NOTI_TYPE_APPROVAL_TURN, o.getNotiType());
        assertEquals(LeaveApprovalNotiConst.SEND_STATUS_PENDING, o.getSendStatus());
        assertEquals("LV_TURN_" + REQ_ID + "_2", o.getDedupKey());
        assertEquals(LeaveApprovalNotiConst.TURN_TITLE, o.getTitle());
        assertEquals("홍길동님이 신청한 연차 결재를 기다리고 있습니다.", o.getBody());
        // payload 에 평문 이름 미포함(라우팅 키만).
        assertTrue(o.getDataPayload().contains("\"reqId\":\"" + REQ_ID + "\""));
        assertTrue(o.getDataPayload().contains("\"approvalStep\":2"));
        assertTrue(!o.getDataPayload().contains(APPLICANT_NM));
    }

    @Test
    @DisplayName("시나리오 A: 결재자가 null/빈값이면 적재 생략(no-op)")
    void approvalTurn_noApprover_noop() {
        service.notifyApprovalTurn(CMPNY, SITE, APPLICANT, REQ_ID, 1, null, APPLICANT);
        service.notifyApprovalTurn(CMPNY, SITE, APPLICANT, REQ_ID, 1, "  ", APPLICANT);
        verify(dashboardMapper, never()).insertNotiOutbox(any());
    }

    @Test
    @DisplayName("시나리오 A: 적재 중 예외가 나도 호출자에게 전파하지 않는다(격리)")
    void approvalTurn_exceptionSwallowed() {
        when(dashboardMapper.insertNotiOutbox(any())).thenThrow(new RuntimeException("DB down"));
        assertDoesNotThrow(() ->
                service.notifyApprovalTurn(CMPNY, SITE, APPLICANT, REQ_ID, 1, "MGR1", APPLICANT));
    }

    // ───────────────────────── 시나리오 B 본문 빌더 ─────────────────────────

    @Test
    @DisplayName("시나리오 B 종일(00): 'X님이 YYYY-MM-DD 연차 N일을 사용했습니다.'")
    void usedBody_full() {
        when(notiMapper.selectNodeAdmins(CMPNY, SITE, APPLICANT)).thenReturn(List.of("MGR1"));

        service.notifyLeaveUsedNoAprv(CMPNY, SITE, APPLICANT, LEAVE_ID, "00",
                new BigDecimal("1.00000"), "20260610", null, null, APPLICANT);

        NotiOutboxInsertVO o = captureFirstOutbox();
        assertEquals("홍길동님이 2026-06-10 연차 1일을 사용했습니다.", o.getBody());
        assertEquals(LeaveApprovalNotiConst.NOTI_TYPE_USED_NO_APRV, o.getNotiType());
        assertEquals("LV_USED_" + LEAVE_ID + "_MGR1", o.getDedupKey());
    }

    @Test
    @DisplayName("시나리오 B 반차(01): 'X님이 YYYY-MM-DD 반차를 사용했습니다.'")
    void usedBody_half() {
        when(notiMapper.selectNodeAdmins(CMPNY, SITE, APPLICANT)).thenReturn(List.of("MGR1"));

        service.notifyLeaveUsedNoAprv(CMPNY, SITE, APPLICANT, LEAVE_ID, "01",
                new BigDecimal("0.50000"), "20260610", null, null, APPLICANT);

        assertEquals("홍길동님이 2026-06-10 반차를 사용했습니다.", captureFirstOutbox().getBody());
    }

    @Test
    @DisplayName("시나리오 B 시간차(02): 'X님이 YYYY-MM-DD HH:MM~HH:MM 시간차 연차를 사용했습니다.'")
    void usedBody_hourly() {
        when(notiMapper.selectNodeAdmins(CMPNY, SITE, APPLICANT)).thenReturn(List.of("MGR1"));

        service.notifyLeaveUsedNoAprv(CMPNY, SITE, APPLICANT, LEAVE_ID, "02",
                new BigDecimal("0.25000"), "20260610", "0900", "1100", APPLICANT);

        assertEquals("홍길동님이 2026-06-10 09:00~11:00 시간차 연차를 사용했습니다.", captureFirstOutbox().getBody());
    }

    @Test
    @DisplayName("시나리오 B 시간차(03/04): 02 와 동일하게 시각 범위 렌더링")
    void usedBody_hourlyVariants_sameAs02() {
        when(notiMapper.selectNodeAdmins(CMPNY, SITE, APPLICANT)).thenReturn(List.of("MGR1"));

        // USE_UNIT_TYPE '03' (1시간 단위)
        service.notifyLeaveUsedNoAprv(CMPNY, SITE, APPLICANT, LEAVE_ID, "03",
                new BigDecimal("0.12500"), "20260610", "0900", "1000", APPLICANT);
        assertEquals("홍길동님이 2026-06-10 09:00~10:00 시간차 연차를 사용했습니다.", captureFirstOutbox().getBody());

        // USE_UNIT_TYPE '04' (30분 단위)
        org.mockito.Mockito.reset(dashboardMapper);
        when(dashboardMapper.selectNextNotiId(CMPNY)).thenReturn("N1");
        service.notifyLeaveUsedNoAprv(CMPNY, SITE, APPLICANT, LEAVE_ID, "04",
                new BigDecimal("0.06250"), "20260610", "0900", "0930", APPLICANT);
        assertEquals("홍길동님이 2026-06-10 09:00~09:30 시간차 연차를 사용했습니다.", captureFirstOutbox().getBody());
    }

    @Test
    @DisplayName("시나리오 B 시간차인데 START/END null: NPE 없이 빈 시각 폴백")
    void usedBody_hourly_nullTimes_noNpe() {
        when(notiMapper.selectNodeAdmins(CMPNY, SITE, APPLICANT)).thenReturn(List.of("MGR1"));

        assertDoesNotThrow(() -> service.notifyLeaveUsedNoAprv(CMPNY, SITE, APPLICANT, LEAVE_ID, "02",
                new BigDecimal("0.25000"), "20260610", null, null, APPLICANT));

        NotiOutboxInsertVO o = captureFirstOutbox();
        // 시각이 null 이어도 본문이 생성되고(NPE 없음), 신청자명/일자/'시간차' 라벨은 포함된다.
        assertTrue(o.getBody().contains(APPLICANT_NM));
        assertTrue(o.getBody().contains("2026-06-10"));
        assertTrue(o.getBody().contains("시간차"));
    }

    // ───────────────────────── 시나리오 B 적재/멱등/제외 ─────────────────────────

    @Test
    @DisplayName("시나리오 B: 노드 관리자 수만큼 outbox 적재")
    void usedNoAprv_perAdminOutbox() {
        when(notiMapper.selectNodeAdmins(CMPNY, SITE, APPLICANT)).thenReturn(List.of("MGR1", "MGR2"));

        service.notifyLeaveUsedNoAprv(CMPNY, SITE, APPLICANT, LEAVE_ID, "00",
                new BigDecimal("1.00000"), "20260610", null, null, APPLICANT);

        verify(dashboardMapper, times(2)).insertNotiOutbox(any());
    }

    @Test
    @DisplayName("시나리오 B: 신청자 본인이 노드 관리자 목록에 있으면 제외(자기 알림 방지)")
    void usedNoAprv_excludesApplicantSelf() {
        when(notiMapper.selectNodeAdmins(CMPNY, SITE, APPLICANT)).thenReturn(List.of(APPLICANT, "MGR2"));

        service.notifyLeaveUsedNoAprv(CMPNY, SITE, APPLICANT, LEAVE_ID, "00",
                new BigDecimal("1.00000"), "20260610", null, null, APPLICANT);

        ArgumentCaptor<NotiOutboxInsertVO> cap = ArgumentCaptor.forClass(NotiOutboxInsertVO.class);
        verify(dashboardMapper, times(1)).insertNotiOutbox(cap.capture());
        assertEquals("MGR2", cap.getValue().getTargetUserCd());
    }

    @Test
    @DisplayName("시나리오 B: 노드 관리자 0명이면 no-op(예외 없음)")
    void usedNoAprv_noAdmins_noop() {
        when(notiMapper.selectNodeAdmins(CMPNY, SITE, APPLICANT)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.notifyLeaveUsedNoAprv(CMPNY, SITE, APPLICANT, LEAVE_ID, "00",
                new BigDecimal("1.00000"), "20260610", null, null, APPLICANT));
        verify(dashboardMapper, never()).insertNotiOutbox(any());
    }

    @Test
    @DisplayName("시나리오 B: 중복(DuplicateKey)은 흡수하고 다음 관리자 진행")
    void usedNoAprv_duplicateAbsorbed() {
        when(notiMapper.selectNodeAdmins(CMPNY, SITE, APPLICANT)).thenReturn(List.of("MGR1", "MGR2"));
        when(dashboardMapper.insertNotiOutbox(any()))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("dup"))
                .thenReturn(1);

        assertDoesNotThrow(() -> service.notifyLeaveUsedNoAprv(CMPNY, SITE, APPLICANT, LEAVE_ID, "00",
                new BigDecimal("1.00000"), "20260610", null, null, APPLICANT));
        verify(dashboardMapper, times(2)).insertNotiOutbox(any());
    }

    @Test
    @DisplayName("시나리오 B: selectNodeAdmins 예외도 호출자에게 전파하지 않는다(격리)")
    void usedNoAprv_exceptionSwallowed() {
        when(notiMapper.selectNodeAdmins(anyString(), anyString(), eq(APPLICANT)))
                .thenThrow(new RuntimeException("DB down"));

        assertDoesNotThrow(() -> service.notifyLeaveUsedNoAprv(CMPNY, SITE, APPLICANT, LEAVE_ID, "00",
                new BigDecimal("1.00000"), "20260610", null, null, APPLICANT));
        verify(dashboardMapper, never()).insertNotiOutbox(any());
    }

    private NotiOutboxInsertVO captureFirstOutbox() {
        ArgumentCaptor<NotiOutboxInsertVO> cap = ArgumentCaptor.forClass(NotiOutboxInsertVO.class);
        verify(dashboardMapper, times(1)).insertNotiOutbox(cap.capture());
        return cap.getValue();
    }
}
