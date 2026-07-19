package com.prafta.common.cmm.aiquota.service;

/**
 * 회사별 월간 AI 토큰 쿼터 서비스(플랫폼-AI-토큰쿼터).
 *
 * <p>한도·사용량은 메인 MySQL 신규 2테이블(TB_AI_TOKEN_QUOTA / TB_AI_TOKEN_USAGE)로 이원 관리한다
 * (AI EC2 Postgres tb_ai_call 감사 로그는 best-effort 라 쿼터 판정 소스로 사용 금지 — 요청서 §3).
 *
 * <p>판정 규칙(§2):
 * <ul>
 *   <li>한도 기준 = 입력 + 출력 토큰의 합. 월 기본값 800,000 (QUOTA 행 미존재 = 기본값).</li>
 *   <li>-1 = 무제한(항상 통과), 0 = 완전 차단(즉시 소진 판정).</li>
 *   <li>사용량 연월 키(USE_YM, KST)는 매월 새 행 — 초기화 배치 불필요.</li>
 *   <li>check 와 record 사이 비원자성으로 인한 약간의 초과는 소프트 리밋 명세(§5) — 결함 아님.</li>
 * </ul>
 */
public interface AiQuotaService {

    /** LLM 호출 직전 게이트. 당월(KST) 사용량(입력+출력 합)이 유효한도 이상이면 AI_429_001 throw. */
    void checkOrThrow(String cmpnyCd);

    /** 쿼터 소진 여부만 판정(비동기 러너/자동 큐잉용 — throw 없이 boolean). */
    boolean isExceeded(String cmpnyCd);

    /**
     * LLM 호출 직후 사용량 누적(REQUIRES_NEW — 호출 서비스 트랜잭션/롤백과 격리).
     * 실패는 WARN 로그 후 삼킨다 — 호출자 응답을 훼손하지 않는다.
     */
    void record(String cmpnyCd, long inputTokens, long outputTokens);
}
