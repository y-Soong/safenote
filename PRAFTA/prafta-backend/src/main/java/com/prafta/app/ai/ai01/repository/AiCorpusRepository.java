package com.prafta.app.ai.ai01.repository;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.prafta.app.ai.ai01.dto.response.RagHit;
import com.prafta.common.error.ai.AiErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.extern.slf4j.Slf4j;

/**
 * pgvector 코퍼스(tb_ai_corpus_chunk) 벡터 검색 리포지토리.
 *
 * <p>aiJdbcTemplate(2차 Postgres 데이터소스, 읽기 전용) 로만 조회한다.
 *    ★ 모든 동적 조건은 <b>파라미터 바인딩</b>으로만 처리한다(문자열 연결 금지 = SQL 주입 방지).
 *    신뢰등급/트랙 필터는 Postgres 배열 리터럴 문자열을 바인딩하고 {@code ?::text[]} 로 캐스팅한다.
 *
 * <p>Postgres 컬럼은 소문자, meta_json 값은 {@code meta_json->>'키'} 로 추출한다.
 *    HNSW 코사인 인덱스 사용을 위해 {@code ORDER BY embedding <=> ?::vector} 를 사용한다.
 */
@Slf4j
@Repository
public class AiCorpusRepository {

    private final JdbcTemplate aiJdbcTemplate;

    public AiCorpusRepository(JdbcTemplate aiJdbcTemplate) {
        this.aiJdbcTemplate = aiJdbcTemplate;
    }

    /* v3.8 출처 메타 보강: tb_ai_corpus_source LEFT JOIN 으로 기관명/원문링크/라이선스만 추가(additive).
       기존 컬럼·필터·정렬 무변경(chunk 컬럼에 alias c. 부여만 — source_id 모호성 방지). */
    private static final String SEARCH_SQL =
        "SELECT c.chunk_id, c.source_id, c.content, c.hazard_text, c.measure_text, c.domain_tag, c.cause_agent, "
      + "       c.source_locator, "
      + "       c.meta_json->>'source_name'      AS source_name, "
      + "       c.meta_json->>'data_reliability' AS data_reliability, "
      + "       c.meta_json->>'track'            AS track, "
      + "       s.source_org, s.source_url, s.license_type, "
      /* prafta-062: 근거 층위(출처 단위 속성) — SELECT 만 추가(additive). WHERE/ORDER BY/바인딩 무변경.
         ★배포 전 pgvector 양 DB 에 prafta-062-evidence-tier-1.sql(DDL) 선적용 필수 — 없으면 검색 전면 실패. */
      + "       s.evidence_tier, "
      /* prafta-062 배포 D: 법령 조문 메타(조문시행일자/조문제목) — SELECT 만 추가(additive).
         meta_json 파생이라 DDL 불요, 법령 외 청크는 키 부재로 null. WHERE/ORDER BY/바인딩 무변경. */
      + "       c.meta_json->>'article_effective_date' AS article_effective_date, "
      + "       c.meta_json->>'article_title'          AS article_title, "
      + "       (c.embedding <=> ?::vector)      AS distance "
      + "  FROM tb_ai_corpus_chunk c "
      + "  LEFT JOIN tb_ai_corpus_source s ON (c.source_id = s.source_id) "
      + " WHERE c.use_yn = 'Y' "
      + "   AND (?::text   IS NULL OR c.domain_tag = ?) "
      + "   AND (?::text[] IS NULL OR c.meta_json->>'data_reliability' = ANY(?::text[])) "
      + "   AND (?::text[] IS NULL OR c.meta_json->>'track'            = ANY(?::text[])) "
      /* prafta-062: 신뢰등급 부정 필터(법령 배제 등). null 이면 미적용 = 종전 동일.
         COALESCE 로 meta 키 부재(null) 행도 배제되지 않게 보존(<> ALL 의 null 전파 방지). */
      + "   AND (?::text[] IS NULL OR COALESCE(c.meta_json->>'data_reliability','') <> ALL(?::text[])) "
      + " ORDER BY c.embedding <=> ?::vector "
      + " LIMIT ?";

    /**
     * 벡터 top-K 검색.
     *
     * @param vecLiteral   질의 벡터 리터럴("[0.1,0.2,...]") — {@code ?::vector} 로 바인딩(SELECT/ORDER BY 2회)
     * @param domainTag    도메인 태그 필터(null 이면 미적용)
     * @param reliabilityLiteral 신뢰등급 pg 배열 리터럴(null 이면 미적용)
     * @param trackLiteral track pg 배열 리터럴(null 이면 미적용)
     * @param reliabilityNotInLiteral 신뢰등급 부정(배제) pg 배열 리터럴(null 이면 미적용 = 종전 동일) — prafta-062
     * @param topK         반환 개수(클램프는 서비스 책임)
     */
    public List<RagHit> search(String vecLiteral,
                               String domainTag,
                               String reliabilityLiteral,
                               String trackLiteral,
                               String reliabilityNotInLiteral,
                               int topK) {

        /* ★slot 순서 = SEARCH_SQL 의 ? 등장 순서와 1:1. 총 11개(종전 9개 + prafta-062 부정필터 2슬롯).
           기존 (1)~(7) 순서 무변경, 신규 (8)(9)는 track 필터 뒤·ORDER BY 앞에 삽입. */
        Object[] args = new Object[] {
            vecLiteral,             // (1) SELECT distance
            domainTag, domainTag,   // (2)(3) domain_tag 필터
            reliabilityLiteral, reliabilityLiteral, // (4)(5) 신뢰등급 필터
            trackLiteral, trackLiteral,             // (6)(7) track 필터
            reliabilityNotInLiteral, reliabilityNotInLiteral, // (8)(9) 신뢰등급 부정 필터(prafta-062)
            vecLiteral,             // (10) ORDER BY
            topK                    // (11) LIMIT
        };

        try {
            return aiJdbcTemplate.query(SEARCH_SQL, RAG_HIT_ROW_MAPPER, args);
        } catch (DataAccessException e) {
            // pgvector 접속/쿼리 장애 → 원인은 서버 로그에만, 클라이언트엔 AI 에러계약(AI_500_001)으로.
            log.error("pgvector 코퍼스 검색 실패", e);
            throw new ApiException(AiErrorCode.AI_500_001);
        }
    }

    /**
     * 행 → RagHit 매핑. 거버넌스 파생값(quotable/modifiable/score)은 행 단위 계산이라 여기서 산출한다.
     * (quotable=항상 true, modifiable=track!='verbatim', score=1-distance)
     */
    private static final RowMapper<RagHit> RAG_HIT_ROW_MAPPER = (rs, rowNum) -> {
        String track = rs.getString("track");
        double distance = rs.getDouble("distance");
        return RagHit.builder()
            .chunkId(rs.getString("chunk_id"))
            .sourceId(rs.getString("source_id"))
            .sourceName(rs.getString("source_name"))
            .sourceOrg(rs.getString("source_org"))
            .sourceUrl(rs.getString("source_url"))
            .licenseType(rs.getString("license_type"))
            .evidenceTier(rs.getString("evidence_tier"))
            .dataReliability(rs.getString("data_reliability"))
            .track(track)
            .quotable(true)
            .modifiable(!"verbatim".equals(track))
            .domainTag(rs.getString("domain_tag"))
            .causeAgent(rs.getString("cause_agent"))
            .content(rs.getString("content"))
            .hazardText(rs.getString("hazard_text"))
            .measureText(rs.getString("measure_text"))
            .sourceLocator(rs.getString("source_locator"))
            .articleEffectiveDate(rs.getString("article_effective_date"))
            .articleTitle(rs.getString("article_title"))
            .distance(distance)
            .score(1.0d - distance)
            .build();
    };
}
