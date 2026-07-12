package com.prafta.web.dashboard.dashboard01.result;

/**
 * 대시보드 근태 탭 A2 정상/비정상 근무 일수 카운트 결과 VO (PRAFTA-DASHBOARD-T2, 조건부 집계 단건).
 * 일 단위 롤업(ABSENT &gt; LATE &gt; EARLY_LEAVE &gt; NORMAL) — 합산 정합:
 * normalCnt + lateCnt + earlyLeaveCnt + absentCnt == targetDayCnt.
 * ⚠ record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서와 1:1 일치 필수.
 */
public record DashAttdStatusCountResult(
    int targetDayCnt    // 판정 대상 계획일 수 (스케줄 근무일, 오늘 이하, 연차/휴일 제외)
    , int normalCnt     // 정상 일수
    , int lateCnt       // 지각 일수 (어느 차수든 LATE)
    , int earlyLeaveCnt // 조퇴 일수 (어느 차수든 EARLY_LEAVE, 지각과 중복 시 지각 우선)
    , int absentCnt     // 미출근 일수 (그날 어느 차수도 출근기록 없음)
){
}
