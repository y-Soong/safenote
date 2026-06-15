package com.prafta.app.admin.dashboard.result;

/**
 * J1-10 (B-5): 순회 점검 카운트 결과(분모=금일 점검 대상 개소, 분자=금일 점검 완료 개소).
 *
 * <p>잠정 정의(미결 §5-1): targetCnt=사업장 활성 체크포인트(USE_YN='Y') 수,
 * completedCnt=금일(WORK_DATE=todayYmd) 답변 존재 distinct CHKPT_CD 수.
 */
public record PatrolCountResult(
      int targetCnt
    , int completedCnt
) {
}
