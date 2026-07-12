package com.prafta.web.dashboard.dashboard01.result;

/**
 * 대시보드 안전 탭 위험성평가 상태별 카운트 결과 VO (PRAFTA-DASHBOARD-T5, 조건부 집계 단건).
 * SYS011: 001=검토요청, 002=개선예정 — 월 조건 없음(Risk_03 목록 건수 일치 축).
 * ⚠ record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서와 1:1 일치 필수.
 */
public record RiskStatusCountResult(
    int reviewRequestCnt  // 검토요청(001) 건수
    , int improvePlanCnt  // 개선예정(002) 건수
){
}
