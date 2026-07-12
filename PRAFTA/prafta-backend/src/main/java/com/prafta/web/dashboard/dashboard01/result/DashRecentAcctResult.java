package com.prafta.web.dashboard.dashboard01.result;

/**
 * 대시보드 안전 탭 최근 사고 결과 VO (PRAFTA-DASHBOARD-T4, 전체 기간 최근 3건).
 * PII 미포함 원칙: 재해자/경위(ACCT_DESC)/EMPLOYER_DESC 관련 컬럼은 담지 않는다.
 * ⚠ record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서와 1:1 일치 필수 (MyBatis record 매핑 함정).
 */
public record DashRecentAcctResult(
    String acctId          // 비 PII 식별자 (ACC+YYYYMMDD+SEQ4)
    , String occurYmd        // TB_ACCT.OCCUR_YMD
    , String occurTime       // TB_ACCT.OCCUR_TIME
    , String occurPlace      // TB_ACCT.OCCUR_PLACE (사업장 내 장소 문자열 — 비 PII)
    , String acctGradeCd     // TB_ACCT.ACCT_GRADE_CD [SYS065]
    , String acctGradeNm     // FNC_CMM_INFO_SRCH(... 'SYS065')
    , String processStatusCd // TB_ACCT.PROCESS_STATUS_CD [SYS066]
    , String processStatusNm // FNC_CMM_INFO_SRCH(... 'SYS066')
){
}
