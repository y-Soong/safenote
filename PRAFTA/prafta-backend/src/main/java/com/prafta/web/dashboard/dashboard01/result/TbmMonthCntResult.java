package com.prafta.web.dashboard.dashboard01.result;

/**
 * 대시보드 안전 탭 TBM 월별 완료 세션 건수 결과 VO (PRAFTA-DASHBOARD-T5).
 * DB 조회는 희소(건수 있는 월만) — 12포인트 0채움은 service 가 수행하며 본 record 를 응답에도 재사용.
 * ⚠ record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서와 1:1 일치 필수.
 */
public record TbmMonthCntResult(
    String ym  // 'YYYY-MM'
    , int cnt  // 해당 월 완료(COMPLETED) 세션 건수
){
}
