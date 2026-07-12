package com.prafta.app.ai.ai01.application.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * LLM 호출 감사·과금 로그(tb_ai_call INSERT 페이로드).
 *
 * <p>★PII: 질의 원문은 저장하지 않는다. 길이({@code queryLen})만 기록한다.
 *    (무염 SHA-256 해시도 저장하지 않는다 — PII 역추적 리스크 제거, 중복분석은 MVP 불요.)
 */
public record AiCallLog(
    String callId,          // UUID
    String userCd,          // JWT gv_userCd
    String cmpnyCd,         // JWT gv_cmpnyCd(과금 귀속)
    String endpoint,        // 'ai01/answer'
    String model,
    int queryLen,           // ★질의 원문 저장 안 함(길이만)
    long inputTokens,
    long outputTokens,
    long cacheReadTokens,
    long cacheCreationTokens,
    BigDecimal costUsd,     // usage×모델단가 서버계산
    List<String> usedChunkIds,
    boolean abstained
) {}
