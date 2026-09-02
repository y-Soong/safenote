package com.prafta.app.ai.ai01.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.app.ai.ai01.application.param.RagSearchParam;
import com.prafta.app.ai.ai01.client.EmbeddingClient;
import com.prafta.app.ai.ai01.dto.response.RagHit;
import com.prafta.app.ai.ai01.dto.response.RagSearchResponse;
import com.prafta.app.ai.ai01.repository.AiCorpusRepository;
import com.prafta.app.ai.ai01.service.Ai01Service;
import com.prafta.common.config.AiProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RAG 검색 서비스 구현.
 *
 * <p>흐름: query 임베딩(TEI) → 벡터 리터럴 조립 → pgvector top-K 조회 → 응답.
 *    topK 는 설정값(prafta.ai.search.*)으로 [1, maxTopK] 클램프(기본값은 default-top-k).
 *    거버넌스 파생값(quotable/modifiable/score)은 리포지토리 RowMapper 에서 산출한다.
 *    검색 전용 경량 로깅(질의 프리뷰·hit 수)만 남긴다(정책서 §6 감사는 Phase 2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Ai01ServiceImpl implements Ai01Service {

    private final EmbeddingClient embeddingClient;
    private final AiCorpusRepository aiCorpusRepository;
    private final AiProperties aiProperties;

    @Override
    public RagSearchResponse search(RagSearchParam param) {

        int topK = clampTopK(param.topK());

        // ★ 질의 원문은 사용자 자유입력이라 PII(실명·연락처 등) 포함 가능 → 로그에 남기지 않고 길이만 기록.
        log.info("RAG 검색 진입 - userCd={}, cmpnyCd={}, topK={}, domainTag={}, queryLen={}"
            , param.gvUserCd(), param.gvCmpnyCd(), topK, param.domainTag(), param.query().length());

        // 1) 질의 임베딩(TEI) → float[1024]
        float[] vector = embeddingClient.embed(param.query());

        // 2) pgvector 리터럴("[v1,v2,...]") 조립
        String vecLiteral = toVectorLiteral(vector);

        // 3) 필터(신뢰등급/트랙)를 Postgres 배열 리터럴로 변환(바인딩 값 — SQL 주입 안전)
        String reliabilityLiteral = toPgTextArrayLiteral(param.reliabilityIn());
        String trackLiteral = toPgTextArrayLiteral(param.trackIn());
        // prafta-062: 신뢰등급 부정 필터(법령 배제 등 서버 내부 전용) — null 이면 미적용 = 종전 동일.
        String reliabilityNotInLiteral = toPgTextArrayLiteral(param.reliabilityNotIn());

        // 4) 검색
        List<RagHit> hits = aiCorpusRepository.search(
            vecLiteral, param.domainTag(), reliabilityLiteral, trackLiteral, reliabilityNotInLiteral, topK);

        log.info("RAG 검색 완료 - userCd={}, hit수={}", param.gvUserCd(), hits.size());

        return RagSearchResponse.builder()
            .query(param.query())
            .hits(hits)
            .build();
    }

    /** topK 클램프: null → 기본값, 그 외 [1, maxTopK]. */
    private int clampTopK(Integer requested) {
        int defaultTopK = aiProperties.getSearch().getDefaultTopK();
        int maxTopK = aiProperties.getSearch().getMaxTopK();
        if (requested == null) {
            return defaultTopK;
        }
        return Math.max(1, Math.min(requested, maxTopK));
    }

    /** float[] → pgvector 리터럴 "[v1,v2,...]". */
    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 12 + 2);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * 문자열 목록 → Postgres 배열 리터럴(예: {@code {"규범형","집계형"}}). null/빈 목록이면 null(필터 미적용).
     * 각 원소는 큰따옴표로 감싸고 내부 백슬래시/큰따옴표를 이스케이프한다.
     * (반환 리터럴 자체는 바인딩 <b>값</b>으로만 전달되므로 SQL 주입에 안전하다 — 배열 파싱 안전만 고려.)
     */
    private String toPgTextArrayLiteral(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        boolean anyElement = false;
        for (String v : values) {
            if (v == null || v.isBlank()) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            String escaped = v.trim()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
            sb.append('"').append(escaped).append('"');
            first = false;
            anyElement = true;
        }
        sb.append('}');
        // 유효 원소가 하나도 없으면(모두 공백) 필터 미적용으로 처리
        return anyElement ? sb.toString() : null;
    }
}
