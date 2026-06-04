package com.prafta.web.leaverefusal.leaverefusal01.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.mapper.LeaveRefusalMapper;
import com.prafta.common.cmm.leave.service.LeaveRefusalConst;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.leaverefusal.leaverefusal01.application.model.LeaveRefusalNoticeModel;
import com.prafta.web.leaverefusal.leaverefusal01.application.param.LeaveRefusalNoticeParam;
import com.prafta.web.leaverefusal.leaverefusal01.dto.response.LeaveRefusalNoticeResponse;

/**
 * PRAFTA-COM-001 — 노무수령거부 통지 발송(기능1) 단위테스트.
 * 권한 게이트(master/hr), 멱등(중복 통지 흡수), 정상 적재(outbox+NOTICED)를 검증한다.
 */
class LeaveRefusalNoticeServiceImplTest {

    private static final String CMPNY = "C001";
    private static final String OPERATOR = "ADMIN1";

    private LeaveRefusalMapper refusalMapper;
    private LeaveDashboardMapper dashboardMapper;
    private LeaveRefusalNoticeServiceImpl service;

    @BeforeEach
    void setUp() {
        refusalMapper = mock(LeaveRefusalMapper.class);
        dashboardMapper = mock(LeaveDashboardMapper.class);
        service = new LeaveRefusalNoticeServiceImpl(refusalMapper, dashboardMapper, new ObjectMapper());
        // 기본: 모든 대상은 정합(실재·활성)으로 가정. 부정합 케이스는 개별 테스트에서 재정의.
        when(refusalMapper.countValidTarget(anyString(), anyString(), anyString())).thenReturn(1);
    }

    private LeaveRefusalNoticeParam param(String authCd, LeaveRefusalNoticeModel... models) {
        return new LeaveRefusalNoticeParam(CMPNY, OPERATOR, authCd, List.of(models));
    }

    private LeaveRefusalNoticeModel model(String userCd, String ymd) {
        return new LeaveRefusalNoticeModel(CMPNY, "S001", userCd, ymd, OPERATOR);
    }

    @Test
    @DisplayName("권한이 master/hr 아니면 403")
    void nonManager_forbidden() {
        LeaveRefusalNoticeParam p = param("999999", model("U001", "20260601"));
        assertThrows(ApiException.class, () -> service.sendNotices(p));
        verify(dashboardMapper, never()).insertNotiOutbox(any());
    }

    @Test
    @DisplayName("정상: 대상별 outbox(PENDING) + NOTICED 로그 적재, 신규건수 반영")
    void manager_insertsOutboxAndLog() {
        when(dashboardMapper.selectNextNotiId(CMPNY)).thenReturn("N1", "N2");
        when(refusalMapper.selectNextRefusalId(CMPNY)).thenReturn("LR1", "LR2");

        LeaveRefusalNoticeResponse res = service.sendNotices(
                param(AuthRoleUtils.AUTH_HR_MANAGER, model("U001", "20260601"), model("U002", "20260601")));

        assertEquals(2, res.getRequestedCount());
        assertEquals(2, res.getNoticedCount());
        verify(dashboardMapper, times(2)).insertNotiOutbox(any(NotiOutboxInsertVO.class));
        verify(refusalMapper, times(2)).insertRefusalLog(any());
    }

    @Test
    @DisplayName("멱등: 중복 통지(outbox DuplicateKey)는 흡수 — NOTICED 로그도 건너뜀, noticedCount 미가산")
    void duplicateNotice_isIdempotent() {
        when(dashboardMapper.selectNextNotiId(CMPNY)).thenReturn("N1");
        when(dashboardMapper.insertNotiOutbox(any()))
                .thenThrow(new DuplicateKeyException("dup"));

        LeaveRefusalNoticeResponse res = service.sendNotices(
                param(AuthRoleUtils.AUTH_MASTER, model("U001", "20260601")));

        assertEquals(1, res.getRequestedCount());
        assertEquals(0, res.getNoticedCount());
        // 중복이면 NOTICED 로그 INSERT 까지 가지 않는다(멱등 종료).
        verify(refusalMapper, never()).insertRefusalLog(any());
    }

    @Test
    @DisplayName("IDOR: 회사 스코프 밖/미존재(siteCd,userCd) 대상 포함 시 통지 전체 거부(0건 INSERT, 예외)")
    void invalidTarget_failsClosed_noInsert() {
        // U002 는 부정합(미존재/회사 밖) → countValidTarget=0.
        when(refusalMapper.countValidTarget(eq(CMPNY), anyString(), eq("U001"))).thenReturn(1);
        when(refusalMapper.countValidTarget(eq(CMPNY), anyString(), eq("U002"))).thenReturn(0);

        LeaveRefusalNoticeParam p =
                param(AuthRoleUtils.AUTH_MASTER, model("U001", "20260601"), model("U002", "20260601"));

        assertThrows(ApiException.class, () -> service.sendNotices(p));
        // 부분 처리 금지: outbox/로그 INSERT 가 한 건도 일어나지 않아야 한다.
        verify(dashboardMapper, never()).insertNotiOutbox(any());
        verify(refusalMapper, never()).insertRefusalLog(any());
    }

    @Test
    @DisplayName("정상 대상(모두 정합)은 기존대로 통지된다")
    void validTargets_proceed() {
        when(dashboardMapper.selectNextNotiId(CMPNY)).thenReturn("N1");
        when(refusalMapper.selectNextRefusalId(CMPNY)).thenReturn("LR1");

        LeaveRefusalNoticeResponse res =
                service.sendNotices(param(AuthRoleUtils.AUTH_MASTER, model("U001", "20260601")));

        assertEquals(1, res.getRequestedCount());
        assertEquals(1, res.getNoticedCount());
        verify(dashboardMapper, times(1)).insertNotiOutbox(any(NotiOutboxInsertVO.class));
        verify(refusalMapper, times(1)).insertRefusalLog(any());
    }

    @Test
    @DisplayName("통지 본문/유형 상수가 적용된다")
    void notice_usesTemplateConstants() {
        when(dashboardMapper.selectNextNotiId(CMPNY)).thenReturn("N1");
        when(refusalMapper.selectNextRefusalId(CMPNY)).thenReturn("LR1");
        org.mockito.ArgumentCaptor<NotiOutboxInsertVO> cap =
                org.mockito.ArgumentCaptor.forClass(NotiOutboxInsertVO.class);

        service.sendNotices(param(AuthRoleUtils.AUTH_MASTER, model("U001", "20260601")));

        verify(dashboardMapper).insertNotiOutbox(cap.capture());
        NotiOutboxInsertVO o = cap.getValue();
        assertEquals(LeaveRefusalConst.NOTI_TYPE_NOTICE, o.getNotiType());
        assertEquals(LeaveRefusalConst.NOTICE_TITLE, o.getTitle());
        assertEquals("LRN_U001_20260601", o.getDedupKey());
    }
}
