package com.prafta.app.ai.ai01.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.app.ai.ai01.application.model.AiCallLog;

import lombok.extern.slf4j.Slf4j;

/**
 * LLM 호출 감사·과금 로그 리포지토리(tb_ai_call INSERT).
 *
 * <p>aiJdbcTemplate(2차 Postgres = prafta_ai) 로 기록한다. 모든 값은 <b>파라미터 바인딩</b>(SQL 주입 차단).
 *    USED_CHUNK_IDS 는 JSON 문자열을 {@code ?::jsonb} 로 캐스팅해 저장한다.
 *
 * <p>★best-effort: 로깅 실패가 답변 성공을 훼손하지 않도록 서비스에서 예외를 삼킨다(경고 로그).
 *    ★PII: 질의 원문은 저장하지 않는다(QUERY_LEN/선택적 해시만).
 */
@Slf4j
@Repository
public class AiCallRepository {

    private final JdbcTemplate aiJdbcTemplate;
    private final ObjectMapper objectMapper;

    public AiCallRepository(JdbcTemplate aiJdbcTemplate, ObjectMapper objectMapper) {
        this.aiJdbcTemplate = aiJdbcTemplate;
        this.objectMapper = objectMapper;
    }

    private static final String INSERT_SQL =
        "INSERT INTO tb_ai_call ("
      + " call_id, user_cd, cmpny_cd, endpoint, model, query_len, "
      + " input_tokens, output_tokens, cache_read_tokens, cache_creation_tokens, "
      + " cost_usd, used_chunk_ids, abstained"
      + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)";

    /** 감사 로그 1건 INSERT. 예외는 호출부(서비스)가 best-effort 로 처리한다. */
    public void save(AiCallLog logRow) {
        String usedChunkIdsJson = toJsonArray(logRow.usedChunkIds());
        aiJdbcTemplate.update(INSERT_SQL,
            logRow.callId(),
            logRow.userCd(),
            logRow.cmpnyCd(),
            logRow.endpoint(),
            logRow.model(),
            logRow.queryLen(),
            logRow.inputTokens(),
            logRow.outputTokens(),
            logRow.cacheReadTokens(),
            logRow.cacheCreationTokens(),
            logRow.costUsd(),
            usedChunkIdsJson,
            logRow.abstained());
    }

    /** 청크ID 목록 → JSON 배열 문자열. 직렬화 실패 시 빈 배열(로그 무결성보다 응답 성공 우선). */
    private String toJsonArray(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? java.util.List.of() : value);
        } catch (Exception e) {
            log.warn("used_chunk_ids JSON 직렬화 실패 - 빈 배열로 대체", e);
            return "[]";
        }
    }
}
