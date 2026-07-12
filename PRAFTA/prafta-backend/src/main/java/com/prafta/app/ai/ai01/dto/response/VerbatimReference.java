package com.prafta.app.ai.ai01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * verbatim(제3유형·변경금지) 청크의 원문+출처 참조.
 *
 * <p>★하드가드 #4: verbatim 청크의 {@code content} 는 <b>LLM 요청 바디에 절대 투입하지 않는다.</b>
 *    LLM 을 거치지 않고 원문 그대로 응답에만 첨부한다(클라이언트가 "원문 인용"으로 별도 표시).
 */
@Getter
@Builder
public class VerbatimReference {

    /** verbatim 청크 식별자. */
    private String chunkId;
    /** 출처명. */
    private String sourceName;
    /** 신뢰등급. */
    private String dataReliability;
    /** 원문(LLM 미가공). */
    private String content;
    /** 원본 역추적 로케이터. */
    private String sourceLocator;
}
