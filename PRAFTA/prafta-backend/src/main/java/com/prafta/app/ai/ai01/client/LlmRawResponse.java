package com.prafta.app.ai.ai01.client;

/**
 * LLM(HyperCLOVA X, HCX-005) 호출의 저수준 결과(파싱 전).
 *
 * <ul>
 *   <li>{@code refusal} — HCX 에는 refusal 개념이 없어 <b>항상 false</b>(레코드 시그니처 호환 유지). true 면 서비스는 content 파싱 없이 abstain 처리하나 도달하지 않는다.</li>
 *   <li>{@code combinedText} — {@code result.message.content} 텍스트.</li>
 *   <li>{@code inputTokens}(=promptTokens)/{@code outputTokens}(=completionTokens) — 감사·과금용 usage.</li>
 *   <li>{@code cacheReadTokens}/{@code cacheCreationTokens} — HCX 는 프롬프트 캐시 과금 없음 → 항상 0.</li>
 * </ul>
 */
public record LlmRawResponse(
    boolean refusal,
    String combinedText,
    long inputTokens,
    long outputTokens,
    long cacheReadTokens,
    long cacheCreationTokens
) {}
