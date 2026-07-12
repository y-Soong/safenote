package com.prafta.app.ai.ai01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RAG 근거답변 요청 본문(JSON @RequestBody, camelCase).
 *
 * <p>검색 요청(RagSearchRequest)과 동일한 필터 계약을 공유한다. 식별자는 본문에서 받지 않는다(JWT 강제).
 *    query 만 필수이며, topK 미지정 시 answer 전용 기본값(prafta.ai.llm.answer-top-k)을 사용한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AnswerRequest {

    /** 검색 질의(필수). */
    private String query;
    /** 근거 청크 개수(옵션). null 이면 answer 전용 기본값(answer-top-k), 상한 초과 시 클램프. */
    private Integer topK;
    /** 도메인 태그 필터(옵션, 단일값). null 이면 전체. */
    private String domainTag;
    /** 신뢰등급 필터(옵션, 예: ["규범형","집계형"]). null/빈값이면 전체. */
    private List<String> reliabilityIn;
    /** 트랙 필터(옵션, 예: ["recompose"]). null/빈값이면 전체. */
    private List<String> trackIn;
}
