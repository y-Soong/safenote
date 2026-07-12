package com.prafta.web.dashboard.dashboard01.result;

/**
 * 대시보드 근태 탭 A1 부서별 근무계획 등록 카운트 결과 VO (PRAFTA-DASHBOARD-T2, 부서당 1행).
 * ⚠ record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서와 1:1 일치 필수.
 */
public record DashAttdPlanRegRateRowResult(
    String nodeCd     // 부서코드 (TB_USER.NODE_CD 귀속)
    , String nodeNm     // 부서명
    , int totalUserCnt  // 부서 대상 사용자 수 (Attd_05 selectUserList 술어 미러)
    , int regUserCnt    // 해당 월 근무계획 등록 사용자 수 (WORK_PLAN_CD IS NOT NULL 하루 이상)
){
}
