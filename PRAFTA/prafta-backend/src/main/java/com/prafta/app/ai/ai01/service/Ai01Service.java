package com.prafta.app.ai.ai01.service;

import com.prafta.app.ai.ai01.application.param.RagSearchParam;
import com.prafta.app.ai.ai01.dto.response.RagSearchResponse;

/**
 * RAG(AI 검색) 서비스.
 * 질의를 임베딩하여 pgvector 코퍼스에서 top-K 를 검색하고, 출처/신뢰등급/트랙을 포함해 반환한다(LLM 없음).
 */
public interface Ai01Service {

    // 검색: query 임베딩 → 벡터 top-K 조회 → 거버넌스 파생값 포함 반환.
    RagSearchResponse search(RagSearchParam param);
}
