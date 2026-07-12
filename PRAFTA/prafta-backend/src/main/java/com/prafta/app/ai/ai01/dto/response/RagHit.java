package com.prafta.app.ai.ai01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * RAG 검색 결과 1건(코퍼스 청크 + 거버넌스 파생값).
 *
 * <p>거버넌스(정책서 §8 정합):
 *   <ul>
 *     <li>{@code quotable} 은 항상 true(검색 결과는 출처와 함께 인용 가능).</li>
 *     <li>{@code modifiable} 은 track != 'verbatim' 일 때만 true(verbatim=제3유형 변경금지, 원문 인용만).</li>
 *     <li>{@code score = 1 - distance}(코사인 거리 → 유사도). 클라이언트 표시 강도 판단용.</li>
 *   </ul>
 */
@Getter
@Builder
public class RagHit {

    /** 청크 식별자. */
    private String chunkId;
    /** 출처(자료) 식별자. */
    private String sourceId;
    /** 출처명(제공기관/데이터셋). meta_json->>'source_name'. */
    private String sourceName;
    /** 제공 기관명(v3.8 — tb_ai_corpus_source.source_org LEFT JOIN, 없으면 null). */
    private String sourceOrg;
    /** 원문 링크(v3.8 — tb_ai_corpus_source.source_url, 없으면 null). */
    private String sourceUrl;
    /** 라이선스 표기(v3.8 — tb_ai_corpus_source.license_type, 없으면 null). */
    private String licenseType;
    /** 신뢰등급(규범형|집계형|자율신고형). meta_json->>'data_reliability'. */
    private String dataReliability;
    /** 트랙(recompose|verbatim). meta_json->>'track'. */
    private String track;
    /** 인용 가능 여부(항상 true). */
    private boolean quotable;
    /** 재구성 가능 여부(track != 'verbatim'). */
    private boolean modifiable;
    /** 도메인 태그. */
    private String domainTag;
    /** 기인물. */
    private String causeAgent;
    /** 본문. */
    private String content;
    /** 유해위험요인 텍스트. */
    private String hazardText;
    /** 감소대책 텍스트. */
    private String measureText;
    /** 원본 역추적 로케이터. */
    private String sourceLocator;
    /** 코사인 거리(embedding <=> query). 작을수록 유사. */
    private double distance;
    /** 유사도 점수(1 - distance). */
    private double score;
}
