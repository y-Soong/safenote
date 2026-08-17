package com.prafta.app.approval.admin.result;

/**
 * 초과근무 승인 상세 — 당일 배정 스케줄 컨텍스트 1행 (2026-08-17 요청).
 *
 * <p>근무일의 TB_USER_WORK_PLAN → 유효버전 스케줄(SchEffective)을 결합해
 * 소정 구간(1/2구간)과 고정연장(전방·후방) 시각을 내려준다.
 * 근무계획이 없거나 WORK_PLAN_CD 가 연차코드면 schCd 가 NULL(스케줄 없음)이다.
 *
 * <p>★ record 위치 기반 매핑 — SELECT 컬럼 순서와 컴포넌트 순서 1:1 유지.
 */
public record DayScheduleRow(
      String schCd
    , String schNo
    , String fstStrTime
    , String fstEndTime
    , String secStrTime
    , String secEndTime
    , String preFixedOtStrTime
    , String preFixedOtEndTime
    , String fixedOtStrTime
    , String fixedOtEndTime
) {
}
