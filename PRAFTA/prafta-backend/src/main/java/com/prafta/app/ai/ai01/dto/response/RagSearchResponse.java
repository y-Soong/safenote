package com.prafta.app.ai.ai01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * RAG 검색 응답. 질의 + 검색 결과 목록(출처/신뢰등급/트랙 포함).
 */
@Getter
@Builder
public class RagSearchResponse {

    /** 원 질의(에코). */
    private String query;
    /** 검색 결과(유사도 오름차순 = distance 오름차순). */
    private List<RagHit> hits;
}
