package com.prafta.web.dashboard.dashboard01.result;

/**
 * 대시보드 안전 탭 무사고 기산점(사고 이력 없는 사업장) 결과 VO (PRAFTA-DASHBOARD-T4).
 * ⚠ record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서와 1:1 일치 필수.
 */
public record DashSiteBaselineResult(
    String strDate   // TB_SITE.STR_DATE 사업개시일 (nullable)
    , String insertYmd // DATE_FORMAT(TB_SITE.INSERT_DATE, '%Y%m%d') 사업장 등록일
){
}
