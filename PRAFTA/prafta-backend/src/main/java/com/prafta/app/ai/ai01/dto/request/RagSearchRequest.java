package com.prafta.app.ai.ai01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RAG 검색 요청 본문(JSON @RequestBody, camelCase).
 *
 * <p>식별자는 본문에서 받지 않는다(인증은 JWT 필터가 강제). query 만 필수이며,
 *    나머지 필터(domainTag / reliabilityIn / trackIn)는 옵션이다.
 */
@Getter
@Setter
@NoArgsConstructor
public class RagSearchRequest {

    /** 검색 질의(필수). */
    private String query;
    /** 반환 개수(옵션). null 이면 서버 기본값, 상한 초과 시 클램프. */
    private Integer topK;
    /** 도메인 태그 필터(옵션, 단일값). null 이면 전체. */
    private String domainTag;
    /** 신뢰등급 필터(옵션, 예: ["규범형","집계형"]). null/빈값이면 전체. */
    private List<String> reliabilityIn;
    /** 트랙 필터(옵션, 예: ["recompose"]). null/빈값이면 전체. */
    private List<String> trackIn;
}
