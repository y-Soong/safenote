package com.prafta.app.ai.ai01.service;

import com.prafta.app.ai.ai01.application.param.AnswerParam;
import com.prafta.app.ai.ai01.dto.response.AnswerResponse;

/**
 * RAG 근거답변 서비스(Phase 2).
 *
 * <p>Phase 1 검색을 재사용해 청크를 얻고, track 으로 물리 분리(하드가드 #4)한 뒤
 *    recompose 청크만 LLM 프롬프트에 투입해 합성 답변을 만든다. verbatim 은 원문으로만 첨부한다.
 */
public interface AnswerService {

    /** 근거답변 생성. recompose 근거가 없거나 refusal 이면 abstained 응답. */
    AnswerResponse answer(AnswerParam param);
}
