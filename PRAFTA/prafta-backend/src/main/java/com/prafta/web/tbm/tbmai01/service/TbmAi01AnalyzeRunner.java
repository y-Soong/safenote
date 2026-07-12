package com.prafta.web.tbm.tbmai01.service;

import java.util.List;

import com.prafta.web.tbm.tbmai01.application.model.TbmAiAnalyzeTarget;

/**
 * TBM 세부항목 비동기 VLM 분석 러너.
 *
 * <p>★별도 @Service 빈으로 분리한다. {@code @Async} 는 스프링 프록시 경유로만 동작하므로
 *    메인 서비스가 자기호출이 아니라 이 빈을 통해 호출해야 한다(자기호출 금지).
 */
public interface TbmAi01AnalyzeRunner {

    /**
     * 지정 항목들을 비동기 VLM 분석해 항목별로 DRAFT/FAILED 로 상태 전이한다.
     *
     * @param targets  분석 대상(이미지 01 / PDF 04)
     * @param cmpnyCd  회사 스코프(JWT 도출)
     * @param userCd   감사 사용자(JWT 도출)
     * @param endpoint 감사 엔드포인트("tbmai01/analyze" 초기 / "tbmai01/reanalyze" 재분석)
     * @param adminNote 재분석 시 관리자 의견(요청단발·미영속 — 프롬프트 컨텍스트로만). 없으면 null
     */
    void runAsync(List<TbmAiAnalyzeTarget> targets, String cmpnyCd, String userCd, String endpoint, String adminNote);
}
