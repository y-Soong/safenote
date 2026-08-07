package com.prafta.web.dashboard.dashboard01.result;

/**
 * NF-2b(2026-08-07): 그날 확정 <b>부분연차(반차 {@code USE_UNIT_TYPE='01'})</b> 1건의 면제 시각 구간.
 *
 * <p>웹 {@code Attd08Mapper/Attd11Mapper.selectHalfLeaveWindows} 와 동일 술어의 미러 조회다.
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서 의존 — {@code Dashboard01Mapper.selectDashPartialLeaveWindows} 와 순서 일치.
 */
public record DashHalfLeaveWindowRow(
        String userCd
        , String workYmd        // = TB_USER_LEAVE_USE.START_DATE (연차 1행 = 하루 불변식)
        , String startTime      // HHmm
        , String endTime        // HHmm
) {
}
