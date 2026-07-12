package com.prafta.app.ai.ai01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * RAG 근거답변 응답(ai01/answer).
 *
 * <ul>
 *   <li>{@code answer} — recompose 청크만으로 합성한 LLM 답변(각주 [n]). abstained 면 빈 문자열.</li>
 *   <li>{@code citations} — 답변 각주 ↔ recompose 청크 매핑(서버 검증 통과분).</li>
 *   <li>{@code verbatimReferences} — verbatim 청크의 원문+출처(LLM 미가공, 하드가드 #4).</li>
 *   <li>{@code usedChunkIds} — 응답에 실제 사용된 청크 ID(인용 recompose + verbatim).</li>
 *   <li>{@code abstained} — 관련 recompose 근거가 없거나 refusal 이라 합성하지 않음.</li>
 * </ul>
 */
@Getter
@Builder
public class AnswerResponse {

    /** abstain 여부(true 면 answer 는 빈 문자열, verbatimReferences 만 제시 가능). */
    private boolean abstained;
    /** 합성 답변(각주 [n] 포함). */
    private String answer;
    /** 각주-청크 매핑. */
    private List<Citation> citations;
    /** verbatim 원문 참조. */
    private List<VerbatimReference> verbatimReferences;
    /** 응답에 사용된 청크 ID 목록. */
    private List<String> usedChunkIds;
    /** 참고용 고지 문구(판단은 사람). */
    private String disclaimer;
}
