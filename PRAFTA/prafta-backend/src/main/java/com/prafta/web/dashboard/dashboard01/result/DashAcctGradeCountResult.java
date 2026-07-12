package com.prafta.web.dashboard.dashboard01.result;

/**
 * 대시보드 안전 탭 조회월 사고 등급별 카운트 결과 VO (PRAFTA-DASHBOARD-T4, 조건부 집계 단건).
 * ⚠ record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서와 1:1 일치 필수.
 */
public record DashAcctGradeCountResult(
    int monthTotalCnt // 조회월 사고 총 건수
    , int grade100Cnt   // 조회월 중대재해(SYS065=100) 건수
    , int grade200Cnt   // 조회월 일반산재(200) 건수
    , int grade300Cnt   // 조회월 신고제외(300) 건수
){
}
