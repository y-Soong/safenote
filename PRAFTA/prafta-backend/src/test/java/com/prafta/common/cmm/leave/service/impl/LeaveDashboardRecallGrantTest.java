package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.leave.vo.LeaveRecallResultVO;
import com.prafta.common.cmm.leave.vo.LeaveRecallTargetVO;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 경력인정 이원화 D-2 재작업(P-11, 2026-08-21) — {@code MANUAL_CAREER} 회수 가드 특례 단위테스트.
 *
 * <p>qa-report D-2: {@code GRANT_TYPE='MANUAL_CAREER'}는 {@code GRANT_BY_TYPE='01'}(자동)로 적재되어
 * 기존 회수 가드({@code GRANT_BY_TYPE='02'} 한정)에 걸려 관리자가 회수할 수 없었다. 본 테스트는 특례 예외가
 * MANUAL_CAREER 한정으로만 열리고, 다른 자동 부여 타입(STATUTORY_*)은 여전히 차단됨을 검증한다.
 */
class LeaveDashboardRecallGrantTest {

    private static final String CMPNY = "C001";
    private static final String AUTH_MASTER = "master";
    private static final String OPERATOR = "OP001";
    private static final String GRANT_ID = "G001";
    private static final String USER = "U001";

    private LeaveDashboardMapper dash;
    private LeaveDashboardServiceImpl svc;

    @BeforeEach
    void setUp() {
        dash = mock(LeaveDashboardMapper.class);
        LeavePolicyService policy = mock(LeavePolicyService.class);
        LeaveGrantEngineService engine = mock(LeaveGrantEngineService.class);
        // 경력인정 이원화 Phase 2 §2-2: 차액 조회 게이트 의존성 추가(본 테스트 경로(회수)와 무관 — mock 주입만)
        svc = new LeaveDashboardServiceImpl(dash, policy, engine, new ObjectMapper(),
                mock(LeaveConversionPolicyService.class),
                mock(SiteAccessService.class));

        when(dash.recallGrant(anyString(), anyString(), anyString(), anyString())).thenReturn(1);
        when(dash.selectNextNotiId(anyString())).thenReturn("N202608210001");
    }

    private LeaveRecallTargetVO target(String grantType, String grantByType) {
        LeaveRecallTargetVO t = new LeaveRecallTargetVO();
        t.setGrantId(GRANT_ID);
        t.setUserCd(USER);
        t.setLeaveCd("SYS_CAREER");
        t.setGrantType(grantType);
        t.setGrantByType(grantByType);
        t.setGrantDays(BigDecimal.valueOf(3));
        t.setUsedDays(BigDecimal.ZERO);
        t.setStatus("ACTIVE");
        t.setDelYn("N");
        return t;
    }

    @Test
    @DisplayName("P-11 특례: MANUAL_CAREER + GRANT_BY_TYPE=01(자동) → 회수 허용")
    void manualCareer_autoGrantByType_recallAllowed() {
        when(dash.selectRecallTarget(eq(CMPNY), eq(GRANT_ID)))
                .thenReturn(target("MANUAL_CAREER", "01"));

        LeaveRecallResultVO result = assertDoesNotThrow(
                () -> svc.recallGrant(CMPNY, GRANT_ID, "오입력 회수", AUTH_MASTER, OPERATOR));

        assertEquals("CANCELED", result.getStatus());
    }

    @Test
    @DisplayName("기존 유지: MANUAL_OTHER(수동 부여) + GRANT_BY_TYPE=02(관리자) → 종전대로 회수 허용")
    void manualOther_adminGrantByType_recallAllowed_unchanged() {
        when(dash.selectRecallTarget(eq(CMPNY), eq(GRANT_ID)))
                .thenReturn(target("MANUAL_OTHER", "02"));

        assertDoesNotThrow(() -> svc.recallGrant(CMPNY, GRANT_ID, "사유", AUTH_MASTER, OPERATOR));
    }

    @Test
    @DisplayName("특례 미적용: STATUTORY_ANNUAL(자동 법정) + GRANT_BY_TYPE=01 → 여전히 차단(ATTD_400_071)")
    void statutoryAnnual_autoGrantByType_stillBlocked() {
        when(dash.selectRecallTarget(eq(CMPNY), eq(GRANT_ID)))
                .thenReturn(target("STATUTORY_ANNUAL", "01"));

        ApiException ex = assertThrows(ApiException.class,
                () -> svc.recallGrant(CMPNY, GRANT_ID, "사유", AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_400_071, ex.getErrorCode());
    }

    @Test
    @DisplayName("특례 미적용: MANUAL_OTHER + GRANT_BY_TYPE=01(자동) → MANUAL_CAREER 아니므로 여전히 차단")
    void manualOther_autoGrantByType_stillBlocked() {
        when(dash.selectRecallTarget(eq(CMPNY), eq(GRANT_ID)))
                .thenReturn(target("MANUAL_OTHER", "01"));

        ApiException ex = assertThrows(ApiException.class,
                () -> svc.recallGrant(CMPNY, GRANT_ID, "사유", AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_400_071, ex.getErrorCode());
    }

    @Test
    @DisplayName("회수 후 재부여: soft cancel UPDATE 성공(row=1) — 멱등키는 grantComponent 의 reactivate 경로가 살아있는 CANCELED 키를 재사용(별도 회귀 없음)")
    void recall_thenReactivateEligible() {
        when(dash.selectRecallTarget(eq(CMPNY), eq(GRANT_ID)))
                .thenReturn(target("MANUAL_CAREER", "01"));

        LeaveRecallResultVO result = svc.recallGrant(CMPNY, GRANT_ID, "오입력 회수", AUTH_MASTER, OPERATOR);

        assertEquals("CANCELED", result.getStatus());
        // 실제 재부여(reactivate) 시나리오는 LeaveGrantEngineServiceImpl.grantComponent()의
        // selectCanceledGrantIdByKey/reactivateCanceledGrant 경로(기존 옵션 A, 회귀 없음)가 담당한다.
        // 본 서비스(recallGrant)는 IDEMPOTENCY_KEY를 건드리지 않고 STATUS만 CANCELED로 전환하므로
        // 재부여 시 grantComponent가 동일 키를 CANCELED 상태로 조회해 reactivate 분기를 그대로 탄다.
        org.mockito.Mockito.verify(dash).recallGrant(eq(CMPNY), eq(GRANT_ID), anyString(), eq(OPERATOR));
    }

    @Test
    @DisplayName("N-1(2차 QA 재검증): 재활성화 후 두 번째 회수도 500 없이 정상 처리(outbox DEDUP_KEY 가 회수 이벤트마다 유일)")
    void secondRecallAfterReactivation_doesNotThrowDuplicateKey() {
        when(dash.selectRecallTarget(eq(CMPNY), eq(GRANT_ID)))
                .thenReturn(target("MANUAL_CAREER", "01"));
        // 실제 채번(selectNextNotiId)은 회수 이벤트마다 다른 값을 반환한다(회사별 날짜+시퀀스).
        when(dash.selectNextNotiId(anyString())).thenReturn("N202608210001", "N202608210002");

        // UK_NOTI_OUTBOX_DEDUP(CMPNY_CD, DEDUP_KEY) 유니크 제약을 흉내낸다 — 같은 DEDUP_KEY 로 두 번째
        // INSERT 를 시도하면 DuplicateKeyException 을 던져, N-1 수정 전 운영에서 실제로 발생했던
        // "Duplicate entry ...RECALL_{grantId}... for key UK_NOTI_OUTBOX_DEDUP" 500 을 재현할 수 있게 한다.
        Set<String> insertedDedupKeys = new HashSet<>();
        doAnswer(invocation -> {
            NotiOutboxInsertVO vo = invocation.getArgument(0);
            if (!insertedDedupKeys.add(vo.getDedupKey())) {
                throw new DuplicateKeyException("Duplicate entry for key UK_NOTI_OUTBOX_DEDUP");
            }
            return 1;
        }).when(dash).insertNotiOutbox(any(NotiOutboxInsertVO.class));

        // 1차 회수(정상).
        LeaveRecallResultVO first = assertDoesNotThrow(
                () -> svc.recallGrant(CMPNY, GRANT_ID, "1차 회수", AUTH_MASTER, OPERATOR));
        assertEquals("CANCELED", first.getStatus());

        // 재활성화(LeaveGrantEngineServiceImpl.grantComponent 의 기존 reactivate 경로 — 본 테스트는 이후
        // 다시 회수 가능한 ACTIVE 상태로 되돌아왔다고 가정) 후 2차 회수.
        LeaveRecallResultVO second = assertDoesNotThrow(
                () -> svc.recallGrant(CMPNY, GRANT_ID, "2차 회수(재활성화 후)", AUTH_MASTER, OPERATOR));
        assertEquals("CANCELED", second.getStatus());

        ArgumentCaptor<NotiOutboxInsertVO> captor = ArgumentCaptor.forClass(NotiOutboxInsertVO.class);
        verify(dash, times(2)).insertNotiOutbox(captor.capture());
        assertNotEquals(captor.getAllValues().get(0).getDedupKey(), captor.getAllValues().get(1).getDedupKey());
    }

    @Test
    @DisplayName("같은 클릭(더블클릭) 중복 방지는 그대로 유지: 두 번째 요청은 outbox 적재 전 UPDATE 0건으로 차단된다")
    void doubleClickSameEvent_stillBlockedBeforeOutboxInsert() {
        when(dash.selectRecallTarget(eq(CMPNY), eq(GRANT_ID)))
                .thenReturn(target("MANUAL_CAREER", "01"));
        // 같은 회수 이벤트에 대한 더블클릭/재전송 — 원자적 UPDATE(WHERE STATUS='ACTIVE')가
        // 두 번째 요청에서는 0건을 반환한다(첫 요청이 이미 CANCELED로 바꿔놓았기 때문).
        when(dash.recallGrant(anyString(), anyString(), anyString(), anyString())).thenReturn(1, 0);

        LeaveRecallResultVO first = assertDoesNotThrow(
                () -> svc.recallGrant(CMPNY, GRANT_ID, "회수", AUTH_MASTER, OPERATOR));
        assertEquals("CANCELED", first.getStatus());

        ApiException ex = assertThrows(ApiException.class,
                () -> svc.recallGrant(CMPNY, GRANT_ID, "회수", AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_409_071, ex.getErrorCode());

        // 더블클릭(같은 이벤트)의 두 번째 요청은 outbox 적재 자체에 도달하지 못한다 — 1회만 호출됨을 확인.
        verify(dash, times(1)).insertNotiOutbox(any(NotiOutboxInsertVO.class));
    }
}
