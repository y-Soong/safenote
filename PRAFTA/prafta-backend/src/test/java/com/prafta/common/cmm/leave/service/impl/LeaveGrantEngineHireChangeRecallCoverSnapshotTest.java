package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.mapper.LeaveGrantEngineMapper;
import com.prafta.common.cmm.leave.service.LeaveGrantStatusService;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.leave.vo.HireDateAdjustResultVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantRecallRowVO;

/**
 * 경력인정 이원화 Phase 2 §2-3 P2-6 ③ — 입사일 변경 회수 스냅샷에서 법정 수기부여({@code _COVER}) 식별 검증.
 *
 * <p>{@code GRANT_TYPE='STATUTORY_ANNUAL'}만으로는 일반 본연차 grant와 {@code _COVER}를 구분할 수 없어
 * (plan §2-6 ③), {@code LeaveGrantRecallRowVO.idempotencyKey}(신설) 기반 {@code coverGrant} 구분 필드를
 * 스냅샷에 추가했다. 본 테스트는 그 필드가 정확히 채워지는지 확인한다.
 */
class LeaveGrantEngineHireChangeRecallCoverSnapshotTest {

    private static final String CMPNY = "C001";
    private static final String USER = "U001";
    private static final String OP = "OP001";

    private LeaveDashboardMapper dash;
    private LeaveGrantEngineMapper eng;
    private LeaveGrantEngineServiceImpl svc;

    @BeforeEach
    void setUp() {
        dash = mock(LeaveDashboardMapper.class);
        eng = mock(LeaveGrantEngineMapper.class);
        LeavePolicyService policySvc = mock(LeavePolicyService.class);
        LeaveGrantStatusService statusSvc = mock(LeaveGrantStatusService.class);
        svc = new LeaveGrantEngineServiceImpl(dash, eng, policySvc, statusSvc);
    }

    @Test
    @DisplayName("회수 스냅샷: _COVER 건은 coverGrant=Y, 일반 본연차 건은 coverGrant=N")
    void snapshot_identifiesCoverGrant() {
        // 현재 10일 → 목표 4일(diff=-6), 회수가능 10일(충분)
        when(eng.selectActiveStatutoryGrantedTotal(eq(CMPNY), eq(USER))).thenReturn(BigDecimal.valueOf(10));
        when(eng.selectRecallableStatutoryTotal(eq(CMPNY), eq(USER))).thenReturn(BigDecimal.valueOf(10));

        LeaveGrantRecallRowVO coverRow = recallRow("G_COVER_1", BigDecimal.valueOf(3), BigDecimal.ZERO,
                "U001_ab12cd34_99999_COVER");
        LeaveGrantRecallRowVO normalRow = recallRow("G_NORMAL_1", BigDecimal.valueOf(10), BigDecimal.ZERO,
                "U001_2026_STATUTORY_ANNUAL");
        when(eng.selectActiveStatutoryGrantsForRecall(eq(CMPNY), eq(USER)))
                .thenReturn(List.of(coverRow, normalRow));
        when(eng.cancelStatutoryGrantForHireChange(eq(CMPNY), eq("G_COVER_1"), anyString(), anyString(), eq(OP)))
                .thenReturn(1);
        when(eng.reduceStatutoryGrantDaysForHireChange(eq(CMPNY), eq("G_NORMAL_1"), any(BigDecimal.class),
                anyString(), eq(OP))).thenReturn(1);

        HireDateAdjustResultVO result = svc.adjustStatutoryGrantsByHireDateChange(
                CMPNY, USER, "20200101", BigDecimal.valueOf(4), "입사일 정정에 따른 회수", "H1", OP);

        String json = result.getAffectedSnapshotJson();
        assertTrue(json != null, "스냅샷 JSON이 생성되어야 한다");
        assertTrue(json.contains("\"grantId\":\"G_COVER_1\"") && json.contains("\"coverGrant\":\"Y\""),
                "_COVER 건은 coverGrant=Y로 식별되어야 한다: " + json);
        assertTrue(json.contains("\"grantId\":\"G_NORMAL_1\"") && json.contains("\"coverGrant\":\"N\""),
                "일반 본연차 건은 coverGrant=N이어야 한다: " + json);
    }

    private LeaveGrantRecallRowVO recallRow(String grantId, BigDecimal grantDays, BigDecimal usedDays,
                                            String idempotencyKey) {
        LeaveGrantRecallRowVO vo = new LeaveGrantRecallRowVO();
        vo.setGrantId(grantId);
        vo.setGrantType("STATUTORY_ANNUAL");
        vo.setGrantDays(grantDays);
        vo.setUsedDays(usedDays);
        vo.setGrantDate("20260101");
        vo.setAvailToDate("20270101");
        vo.setIdempotencyKey(idempotencyKey);
        return vo;
    }
}
