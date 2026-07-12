package com.prafta.web.risk.riskai01.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * verbatim 트랙 참고 원문 1건(그라운딩 품질 개선 C — LLM 미경유 서버 패스스루).
 *
 * <p>★하드가드 #4: verbatim 원문은 LLM 프롬프트/컨텍스트에 절대 투입하지 않는다.
 *    서버가 RAG 검색 결과에서 원문 무변경으로 직접 응답에 전달만 하며,
 *    FE 는 "라이선스에 따라 원문 그대로 표시(변경 불가)" 안내와 함께 렌더한다.
 *
 * <p>v3.8: {@code sourceOrg}(기관명)/{@code sourceUrl}(원문 링크)/{@code licenseType}(라이선스 표기)
 *    추가 — tb_ai_corpus_source JOIN 메타. 구 저장분(5필드)은 누락 필드 null 파싱으로 하위호환
 *    (FE 는 v-if 로 생략 방어).
 * <p>응답 DTO 겸 VERBATIM_JSON 저장 스키마로 공용한다(CitationItem 패턴).
 *    chunkId 는 응답에 불필요하여 담지 않는다(내부 로깅 전용).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VerbatimRefItem(
    String sourceName
    , String dataReliability
    , String content
    , String hazardText
    , String measureText
    , String sourceOrg
    , String sourceUrl
    , String licenseType
) {}
