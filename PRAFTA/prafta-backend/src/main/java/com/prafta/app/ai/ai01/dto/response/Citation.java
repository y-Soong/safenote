package com.prafta.app.ai.ai01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 답변 각주 ↔ recompose 청크 매핑 1건.
 *
 * <p>LLM 이 돌려준 각주(marker/chunkId)를 서버가 <b>실제 recompose 청크 집합과 대조</b>해 검증한 뒤,
 *    출처/신뢰등급을 서버 측 청크 메타로 채워 반환한다(환각 인용 차단).
 */
@Getter
@Builder
public class Citation {

    /** 각주 마커(예: "[1]"). */
    private String marker;
    /** 인용된 recompose 청크 식별자(서버 검증 통과분만). */
    private String chunkId;
    /** 출처명(서버 메타). */
    private String sourceName;
    /** 원본 역추적 로케이터(서버 메타). */
    private String sourceLocator;
    /** 신뢰등급(서버 메타). */
    private String dataReliability;
}
