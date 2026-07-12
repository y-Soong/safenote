package com.prafta.web.dashboard.dashboard01.result;

/**
 * 대시보드 안전 탭 순회점검 당일 이행 현황 결과 VO (PRAFTA-DASHBOARD-T5, 조건부 집계 단건).
 * ⚠ record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서와 1:1 일치 필수.
 */
public record PatrolTodayResult(
    int todayInspectCnt // 오늘 점검 기록이 있는 개소 수 (x)
    , int todayTotalCnt // 오늘 기준 사용중(USE_YN='Y') 개소 수 (y)
){
}
