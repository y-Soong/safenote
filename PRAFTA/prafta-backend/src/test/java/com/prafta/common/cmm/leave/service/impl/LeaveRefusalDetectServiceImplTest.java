package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.mapper.LeaveRefusalMapper;
import com.prafta.common.cmm.leave.service.LeaveRefusalConst;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.cmm.leave.vo.RefusalLogInsertVO;
import com.prafta.common.cmm.leave.vo.RefusalTargetVO;

/**
 * PRAFTA-COM-001 — 노무수령거부 출근 감지/관리자 PUSH(기능2/3) 단위테스트.
 *
 * <p>매퍼는 Mockito mock. 휴일 게이트는 매퍼 SQL(NOT EXISTS) 책임이므로 여기서는
 * "대상일 판정 결과(selectRefusalTarget)가 null 이면 감지 skip" 으로 게이트를 표현한다.
 * (휴일이면 selectRefusalTarget 가 0행 → null 반환.)
 */
class LeaveRefusalDetectServiceImplTest {

    private static final String CMPNY = "C001";
    private static final String SITE = "S001";
    private static final String USER = "U001";
    private static final String NODE = "N001";
    private static final String WORK_YMD = "20260601";
    private static final String ATTD_ID = "A20260601001";

    private LeaveRefusalMapper refusalMapper;
    private LeaveDashboardMapper dashboardMapper;
    private LeaveRefusalDetectServiceImpl service;

    @BeforeEach
    void setUp() {
        refusalMapper = org.mockito.Mockito.mock(LeaveRefusalMapper.class);
        dashboardMapper = org.mockito.Mockito.mock(LeaveDashboardMapper.class);
        service = new LeaveRefusalDetectServiceImpl(refusalMapper, dashboardMapper, new ObjectMapper());
    }

    private RefusalTargetVO target() {
        RefusalTargetVO t = new RefusalTargetVO();
        t.setRefusalId("LR20260601001");
        t.setTargetYmd(WORK_YMD);
        return t;
    }

    @Test
    @DisplayName("대상일이 휴일(=판정결과 null)이면 감지/알림 모두 skip — 로그 INSERT 없음")
    void holidayGate_skipsDetection() {
        // 휴일 게이트로 selectRefusalTarget 가 null (NOT EXISTS 통과 실패).
        when(refusalMapper.selectRefusalTarget(CMPNY, SITE, USER, WORK_YMD)).thenReturn(null);

        service.detectAndAlert(CMPNY, SITE, USER, NODE, WORK_YMD, ATTD_ID, USER);

        verify(refusalMapper, never()).insertRefusalLog(any());
        verify(refusalMapper, never()).selectSiteRefusalAdmins(anyString(), anyString(), anyString(), anyList());
        verify(dashboardMapper, never()).insertNotiOutbox(any());
    }

    @Test
    @DisplayName("미통지(=판정결과 null)면 감지/알림 skip")
    void notNoticed_skipsDetection() {
        when(refusalMapper.selectRefusalTarget(CMPNY, SITE, USER, WORK_YMD)).thenReturn(null);

        service.detectAndAlert(CMPNY, SITE, USER, NODE, WORK_YMD, ATTD_ID, USER);

        verify(refusalMapper, never()).insertRefusalLog(any());
    }

    @Test
    @DisplayName("대상일이면 CHECKIN_DETECTED 기록 + 관리자 PUSH(outbox) + ADMIN_ALERTED 기록")
    void target_recordsAndAlerts() {
        when(refusalMapper.selectRefusalTarget(CMPNY, SITE, USER, WORK_YMD)).thenReturn(target());
        when(refusalMapper.selectNextRefusalId(CMPNY)).thenReturn("LR1", "LR2");
        when(refusalMapper.selectSiteRefusalAdmins(eq(CMPNY), eq(SITE), eq(USER), anyList()))
                .thenReturn(List.of("MGR1", "MGR2"));
        when(dashboardMapper.selectNextNotiId(CMPNY)).thenReturn("N1", "N2");

        service.detectAndAlert(CMPNY, SITE, USER, NODE, WORK_YMD, ATTD_ID, USER);

        // 사실 로그 2건(CHECKIN_DETECTED + ADMIN_ALERTED).
        ArgumentCaptor<RefusalLogInsertVO> logCap = ArgumentCaptor.forClass(RefusalLogInsertVO.class);
        verify(refusalMapper, times(2)).insertRefusalLog(logCap.capture());
        List<RefusalLogInsertVO> logs = logCap.getAllValues();
        org.junit.jupiter.api.Assertions.assertEquals(
                LeaveRefusalConst.EVENT_CHECKIN_DETECTED, logs.get(0).getEventType());
        org.junit.jupiter.api.Assertions.assertEquals(ATTD_ID, logs.get(0).getRelatedAttdId());
        org.junit.jupiter.api.Assertions.assertTrue(logs.get(0).isDetectNow());
        org.junit.jupiter.api.Assertions.assertEquals(
                LeaveRefusalConst.EVENT_ADMIN_ALERTED, logs.get(1).getEventType());

        // 관리자 2명 → outbox 2건, NOTI_TYPE/DEDUP 확인.
        ArgumentCaptor<NotiOutboxInsertVO> outCap = ArgumentCaptor.forClass(NotiOutboxInsertVO.class);
        verify(dashboardMapper, times(2)).insertNotiOutbox(outCap.capture());
        for (NotiOutboxInsertVO o : outCap.getAllValues()) {
            org.junit.jupiter.api.Assertions.assertEquals(LeaveRefusalConst.NOTI_TYPE_CHECKIN_ALERT, o.getNotiType());
            org.junit.jupiter.api.Assertions.assertEquals(LeaveRefusalConst.SEND_STATUS_PENDING, o.getSendStatus());
            org.junit.jupiter.api.Assertions.assertTrue(o.getDedupKey().startsWith("LRA_"));
        }
    }

    @Test
    @DisplayName("관리자 0명이어도 CHECKIN_DETECTED 는 기록되고 예외 없이 종료")
    void noAdmins_stillRecordsDetection() {
        when(refusalMapper.selectRefusalTarget(CMPNY, SITE, USER, WORK_YMD)).thenReturn(target());
        when(refusalMapper.selectNextRefusalId(CMPNY)).thenReturn("LR1");
        when(refusalMapper.selectSiteRefusalAdmins(eq(CMPNY), eq(SITE), eq(USER), anyList()))
                .thenReturn(List.of());

        service.detectAndAlert(CMPNY, SITE, USER, NODE, WORK_YMD, ATTD_ID, USER);

        // CHECKIN_DETECTED 1건만(ADMIN_ALERTED 없음).
        verify(refusalMapper, times(1)).insertRefusalLog(any());
        verify(dashboardMapper, never()).insertNotiOutbox(any());
    }

    @Test
    @DisplayName("내부 매퍼 예외가 발생해도 detectAndAlert 는 예외를 던지지 않는다(체크인 격리)")
    void internalException_isSwallowed() {
        when(refusalMapper.selectRefusalTarget(CMPNY, SITE, USER, WORK_YMD))
                .thenThrow(new RuntimeException("DB down"));

        assertDoesNotThrow(() ->
                service.detectAndAlert(CMPNY, SITE, USER, NODE, WORK_YMD, ATTD_ID, USER));

        verify(refusalMapper, never()).insertRefusalLog(any());
    }

    @Test
    @DisplayName("관리자 PUSH 중복(DuplicateKey)은 흡수하고 다음 관리자 진행 — 예외 없음")
    void duplicateOutbox_isAbsorbed() {
        when(refusalMapper.selectRefusalTarget(CMPNY, SITE, USER, WORK_YMD)).thenReturn(target());
        when(refusalMapper.selectNextRefusalId(CMPNY)).thenReturn("LR1", "LR2");
        when(refusalMapper.selectSiteRefusalAdmins(eq(CMPNY), eq(SITE), eq(USER), anyList()))
                .thenReturn(List.of("MGR1", "MGR2"));
        when(dashboardMapper.selectNextNotiId(CMPNY)).thenReturn("N1", "N2");
        // 첫 관리자 outbox 는 중복(DuplicateKey), 둘째는 정상.
        when(dashboardMapper.insertNotiOutbox(any()))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("dup"))
                .thenReturn(1);

        assertDoesNotThrow(() ->
                service.detectAndAlert(CMPNY, SITE, USER, NODE, WORK_YMD, ATTD_ID, USER));

        // 두 관리자 모두 시도되었고, ADMIN_ALERTED 대표 로그도 기록.
        verify(dashboardMapper, times(2)).insertNotiOutbox(any());
        verify(refusalMapper, times(2)).insertRefusalLog(any());
    }
}
