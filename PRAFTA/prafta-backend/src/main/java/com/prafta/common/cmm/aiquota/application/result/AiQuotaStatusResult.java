package com.prafta.common.cmm.aiquota.application.result;

/**
 * 회사 쿼터 상태 1행(유효한도·당월 사용량 1회 조회 결과).
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서에 의존한다 — 본 컴포넌트 순서와
 * AiQuotaMapper.xml selectQuotaStatus 의 SELECT 순서를 항상 일치시킬 것.
 *
 * @param monthlyTokenLimit QUOTA 행의 한도(-1 무제한 / 0 차단 / 양수). 행 미존재 시 null(기본 800,000 적용).
 * @param usedTokens        당월(USE_YM) 입력+출력 토큰 합. 사용 이력 없으면 null(0 취급).
 */
public record AiQuotaStatusResult(
    Long monthlyTokenLimit
    , Long usedTokens
) {
}
