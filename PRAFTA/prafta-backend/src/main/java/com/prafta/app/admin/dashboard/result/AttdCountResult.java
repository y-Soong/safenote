package com.prafta.app.admin.dashboard.result;

/**
 * J1-10 (B-5): 근태 카운트 결과(출근 실제/예정/연차) — 한 번의 쿼리로 3개 카운트 반환.
 */
public record AttdCountResult(
      int checkedInCnt
    , int scheduledCnt
    , int leaveCnt
) {
}
