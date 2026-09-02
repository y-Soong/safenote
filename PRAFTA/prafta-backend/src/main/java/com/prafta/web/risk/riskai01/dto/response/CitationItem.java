package com.prafta.web.risk.riskai01.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 근거 출처 1건(서버 검증 통과분). {@code marker} 는 각주([n]), {@code sourceName}/{@code dataReliability} 는
 * 코퍼스 청크의 서버 메타(환각 방지 위해 LLM 값이 아닌 서버 대조값을 채운다).
 *
 * <p>v3.8: {@code sourceOrg}(기관명)/{@code sourceUrl}(원문 링크) 추가 — tb_ai_corpus_source JOIN 메타.
 *    구 저장분(CITATION_JSON 3필드)은 {@code @JsonIgnoreProperties} + 누락 필드 null 파싱으로 하위호환
 *    (FE 는 v-if 로 생략 방어).
 * <p>prafta-062: {@code evidenceTier}(층위 코드)/{@code evidenceTierLabel}(표시 문구 — 서버 완성값) 추가.
 *    미지정이면 둘 다 null(FE 배지 미표시). 구 저장분은 동일 방식으로 null 파싱(마이그레이션 불요).
 * <p>응답 DTO 겸 CITATION_JSON 저장 스키마로 공용한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CitationItem(
    String marker
    , String sourceName
    , String dataReliability
    , String sourceOrg
    , String sourceUrl
    , String evidenceTier
    , String evidenceTierLabel
) {}
