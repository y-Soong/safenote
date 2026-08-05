package com.prafta.platform.customer.application.result;

/**
 * 회사별 월간 AI 토큰 사용량 이력 1행(TB_AI_TOKEN_USAGE).
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서에 의존한다 — 본 컴포넌트 순서와
 * PlatformCustomerMapper.xml selectTokenUsageList 의 SELECT 순서를 항상 일치시킬 것.
 *
 * @param useYm       사용 연월(YYYYMM, KST 기준 — 쿼터 서비스와 동일 기준).
 * @param callCnt     LLM 실호출 건수 월 누계.
 * @param inputTokens 입력 토큰 월 누계.
 * @param outputTokens 출력 토큰 월 누계.
 * @param totalTokens 입력+출력 합(SQL 파생 — 한도 판정 기준과 동일 정의).
 */
public record TokenUsageListResult(
    String useYm
    , Integer callCnt
    , Long inputTokens
    , Long outputTokens
    , Long totalTokens
) {
}
