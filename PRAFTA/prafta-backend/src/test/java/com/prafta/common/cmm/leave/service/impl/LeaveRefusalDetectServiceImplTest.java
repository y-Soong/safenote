package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.prafta.common.cmm.leave.mapper.LeaveRefusalMapper;
import com.prafta.common.cmm.leave.service.LeaveRefusalConst;
import com.prafta.common.cmm.leave.vo.RefusalTargetVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * PRAFTA-COM-008-B — 노무수령거부 원천 차단 가드(guardAndRecord) 단위테스트.
 *
 * <p>com-001 detect→block 전환으로 기존 {@code detectAndAlert}(사후 감지) 테스트를 차단 가드 기준으로
 * 재작성했다. 판정({@code selectLaborRefusalTarget})·증빙 적재({@code LeaveRefusalBlockRecorder},
 * REQUIRES_NEW)는 Mockito mock 으로 격리하고 본 테스트는 가드의 분기/차단 throw 만 검증한다:
 * <ul>
 *   <li>비대상(판정 null) → 증빙 미적재 + 차단 throw 없음(호출부 정상 진행).</li>
 *   <li>대상(판정 1건) → 증빙 1회 선커밋(recordBlockAndAlert) + ATTD_400_150 차단 throw.</li>
 *   <li>증빙 적재 실패가 발생해도(흡수) 차단 throw 는 항상 수행("막되 기록"의 막기 보장).</li>
 * </ul>
 * 휴일/법정/촉진 게이트는 매퍼 SQL(NOT EXISTS·LIKE·IN) 책임이므로 여기서는 "판정 null/1건"으로 표현한다.
 */
class LeaveRefusalDetectServiceImplTest {

    private static final String CMPNY = "C001";
    private static final String SITE = "S001";
    private static final String USER = "U001";
    private static final String NODE = "N001";
    private static final String WORK_YMD = "20260601";
    private static final String LEAVE_ID = "LU20260601001";
    private static final String OPERATOR = "U001";

    private LeaveRefusalMapper refusalMapper;
    private LeaveRefusalBlockRecorder recorder;
    private LeaveRefusalDetectServiceImpl service;

    @BeforeEach
    void setUp() {
        refusalMapper = org.mockito.Mockito.mock(LeaveRefusalMapper.class);
        recorder = org.mockito.Mockito.mock(LeaveRefusalBlockRecorder.class);
        service = new LeaveRefusalDetectServiceImpl(refusalMapper, recorder);
    }

    private RefusalTargetVO target() {
        RefusalTargetVO t = new RefusalTargetVO();
        t.setLeaveId(LEAVE_ID);
        t.setTargetYmd(WORK_YMD);
        return t;
    }

    @Test
    @DisplayName("비대상(자발/비법정/휴일/연차없음 → 판정 null)이면 증빙 미적재 + 차단 throw 없음")
    void notTarget_passesThrough() {
        when(refusalMapper.selectLaborRefusalTarget(CMPNY, SITE, USER, WORK_YMD)).thenReturn(null);

        assertDoesNotThrow(() -> service.guardAndRecord(
                CMPNY, SITE, USER, NODE, WORK_YMD,
                LeaveRefusalConst.ATTEMPT_CHECK_IN, OPERATOR));

        verify(recorder, never()).recordBlockAndAlert(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("대상이면 증빙(recordBlockAndAlert) 선커밋 1회 + ATTD_400_150 차단 throw")
    void target_recordsThenThrows() {
        when(refusalMapper.selectLaborRefusalTarget(CMPNY, SITE, USER, WORK_YMD)).thenReturn(target());

        ApiException ex = assertThrows(ApiException.class, () -> service.guardAndRecord(
                CMPNY, SITE, USER, NODE, WORK_YMD,
                LeaveRefusalConst.ATTEMPT_CHECK_IN, OPERATOR));

        assertEquals(AttdErrorCode.ATTD_400_150.code(), ex.getErrorCode().code());
        verify(recorder, times(1)).recordBlockAndAlert(
                eq(CMPNY), eq(SITE), eq(USER), eq(WORK_YMD),
                eq(LEAVE_ID), eq(LeaveRefusalConst.ATTEMPT_CHECK_IN), eq(OPERATOR));
    }

    @Test
    @DisplayName("증빙 적재가 실패해도(흡수) 차단 throw 는 항상 수행 — '막되 기록'의 막기 보장")
    void recorderFailure_stillThrows() {
        when(refusalMapper.selectLaborRefusalTarget(CMPNY, SITE, USER, WORK_YMD)).thenReturn(target());
        doThrow(new RuntimeException("outbox down")).when(recorder).recordBlockAndAlert(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        ApiException ex = assertThrows(ApiException.class, () -> service.guardAndRecord(
                CMPNY, SITE, USER, NODE, WORK_YMD,
                LeaveRefusalConst.ATTEMPT_ADMIN_ENTRY, OPERATOR));

        assertEquals(AttdErrorCode.ATTD_400_150.code(), ex.getErrorCode().code());
        verify(recorder, times(1)).recordBlockAndAlert(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
