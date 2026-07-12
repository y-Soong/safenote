package com.prafta.web.dashboard.dashboard01.result;

/**
 * 대시보드 근태 탭 A3 초과근무 월별 인정시간 합계 결과 VO (PRAFTA-DASHBOARD-T3).
 * DB 조회는 희소(초과근무 존재 월만) — 6포인트 0채움과 'YYYYMM'→'YYYY-MM' 변환은 service 가 수행하며
 * 본 record 를 응답에도 재사용한다(응답 ym 은 'YYYY-MM').
 * ⚠ record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서와 1:1 일치 필수.
 */
public record OvertimeMonthlyResult(
    String ym          // DB: 'YYYYMM' (SUBSTRING) / 응답: 'YYYY-MM' (service 변환)
    , long totalMinutes // 해당 월 SUM(WORK_MINUTES) — 분 단위 인정시간(휴게 제외)
){
}
