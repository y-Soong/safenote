package com.prafta.app.ai.ai01.application.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * LLM 이 지정 JSON 스키마로 반환하는 근거답변 파싱 결과(내부 모델).
 *
 * <p>시스템 프롬프트로 "이 JSON 객체만 출력" 을 강제하고, 결합 텍스트에서 JSON 을 추출해 Jackson 으로 파싱한다.
 *    파싱 실패 시 AI_502_004. 미지의 필드는 무시한다(스키마 유연성).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnswerResult(
    boolean abstained,
    String answer,
    List<CitationOut> citations
) {

    /** LLM 각주 원자값: 마커 + 청크ID. 서버가 recompose 집합과 대조해 검증한다(환각 인용 차단). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CitationOut(
        String marker,
        String chunkId
    ) {}
}
