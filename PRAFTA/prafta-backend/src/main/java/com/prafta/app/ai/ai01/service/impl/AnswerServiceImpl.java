package com.prafta.app.ai.ai01.service.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.app.ai.ai01.application.model.AiCallLog;
import com.prafta.app.ai.ai01.application.model.AnswerResult;
import com.prafta.app.ai.ai01.application.param.AnswerParam;
import com.prafta.app.ai.ai01.application.param.RagSearchParam;
import com.prafta.app.ai.ai01.client.LlmAnswerClient;
import com.prafta.app.ai.ai01.client.LlmRawResponse;
import com.prafta.app.ai.ai01.dto.response.AnswerResponse;
import com.prafta.app.ai.ai01.dto.response.Citation;
import com.prafta.app.ai.ai01.dto.response.RagHit;
import com.prafta.app.ai.ai01.dto.response.RagSearchResponse;
import com.prafta.app.ai.ai01.dto.response.VerbatimReference;
import com.prafta.app.ai.ai01.repository.AiCallRepository;
import com.prafta.app.ai.ai01.service.Ai01Service;
import com.prafta.app.ai.ai01.service.AnswerService;
import com.prafta.common.cmm.aiquota.service.AiQuotaService;
import com.prafta.common.config.AiProperties;
import com.prafta.common.error.ai.AiErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RAG 근거답변 서비스 구현(Phase 2).
 *
 * <p>흐름: Phase 1 검색 재사용 → track 물리 분리(하드가드 #4) → recompose 만 LLM 컨텍스트 →
 *    refusal/파싱/citation 검증 → 감사·과금 로깅(best-effort) → 응답 조립.
 *
 * <p>★하드가드 #4: verbatim 청크의 content 는 프롬프트 조립에 절대 사용하지 않는다
 *    ({@link #buildContextPrompt}는 recompose 리스트만 인자로 받는다). verbatim 은 응답 참조로만 첨부.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerServiceImpl implements AnswerService {

    private final Ai01Service ai01Service;
    private final LlmAnswerClient llmAnswerClient;
    private final AiCallRepository aiCallRepository;
    private final AiQuotaService aiQuotaService;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    private static final String ENDPOINT = "ai01/answer";
    /** ★하드가드 #4 fail-closed 기준: LLM 에는 track 이 명시적으로 recompose 로 확정된 청크만 투입한다. */
    private static final String RECOMPOSE_TRACK = "recompose";

    /**
     * 법령 신뢰등급 값(prafta-062 D5-2). 법령 조문(track=verbatim, data_reliability='법령')이 적재되면
     * 아래 track 물리 분리(97~105행)가 이를 전부 verbatim 참조로 강등해 앱 AI 답변의 참고 원문에
     * 조문이 자동으로 섞여 들어온다 → 검색 단계에서 부정 필터로 배제(앱 미노출 확정 — 무회귀 우선).
     */
    private static final String LAW_RELIABILITY = "법령";

    private static final String DISCLAIMER = "본 내용은 참고 근거이며 최종 판단은 담당자가 수행합니다.";

    /** §6 그라운딩 시스템 프롬프트(요청마다 동일 → 프롬프트 캐시 적중). */
    private static final String SYSTEM_PROMPT =
          "당신은 산업안전 근거 검색 보조자다. 아래 원칙을 반드시 지킨다.\n"
        + "1. 제공된 [근거 청크]에 있는 내용만으로 답한다. 청크에 없는 사실·수치·대책을 지어내지 않는다.\n"
        + "2. 답변의 모든 핵심 문장 끝에 근거 청크 번호를 [n] 형식으로 표기한다.\n"
        + "3. 관련 있는 근거가 없으면 답변을 만들지 말고 abstained=true 로 반환한다.\n"
        + "4. 근거의 신뢰등급을 반영한다: '자율신고형'은 단정하지 말고 \"참고 신고사례에 따르면…\"처럼 약하게 인용한다.\n"
        + "5. 이 답변은 참고용 근거 제시이며 최종 판단은 담당자가 한다. 결정을 단정하지 않는다.\n"
        + "6. citations 에는 실제 인용한 청크의 번호(marker)와 chunkId 만 담는다.\n"
        + "출력은 다음 JSON 객체 하나만 반환하고 다른 텍스트는 절대 포함하지 않는다:\n"
        + "{\"abstained\": <boolean>, \"answer\": \"<각주 [n] 포함 답변, abstained 면 빈 문자열>\", "
        + "\"citations\": [{\"marker\": \"[n]\", \"chunkId\": \"<컨텍스트에서 부여받은 chunkId>\"}]}";

    @Override
    public AnswerResponse answer(AnswerParam param) {

        // ★질의 원문은 PII 가능 → 길이만 로깅(저장도 길이만).
        log.info("RAG 답변 진입 - userCd={}, cmpnyCd={}, queryLen={}, domainTag={}"
            , param.gvUserCd(), param.gvCmpnyCd(), param.query().length(), param.domainTag());

        // 1) Phase 1 검색 재사용(answer 전용 topK 적용).
        int effectiveTopK = (param.topK() != null) ? param.topK() : aiProperties.getLlm().getAnswerTopK();
        // prafta-062 D5-2: 법령 조문은 앱 AI 답변에 미노출 — 신뢰등급 부정 필터로 배제.
        RagSearchParam searchParam = new RagSearchParam(
            param.query(), effectiveTopK, param.domainTag(),
            param.reliabilityIn(), param.trackIn(), List.of(LAW_RELIABILITY),
            param.gvUserCd(), param.gvCmpnyCd());
        RagSearchResponse searchResp = ai01Service.search(searchParam);
        List<RagHit> hits = (searchResp.getHits() == null) ? List.of() : searchResp.getHits();

        // 2) ★track 물리 분리(하드가드 #4) — fail-closed.
        //    track 이 명시적으로 "recompose" 인 청크만 LLM 컨텍스트로 보낸다.
        //    그 외(verbatim/null/미상/오탈자/대소문자 차이)는 전부 verbatim 참조 경로로 강등해
        //    변경금지(제3유형) 원문이 LLM 프롬프트에 유입되는 것을 데이터 위생과 무관하게 코드로 원천 차단한다.
        List<RagHit> recompose = new ArrayList<>();
        List<RagHit> verbatim = new ArrayList<>();
        for (RagHit h : hits) {
            if (RECOMPOSE_TRACK.equals(h.getTrack())) {
                recompose.add(h);
            } else {
                verbatim.add(h);
            }
        }
        List<VerbatimReference> verbatimRefs = toVerbatimReferences(verbatim);

        // 3) recompose 0개 → LLM 호출 없이 abstain(비용 0).
        if (recompose.isEmpty()) {
            log.info("RAG 답변 abstain - recompose 근거 없음(verbatim={}건)", verbatim.size());
            List<String> usedChunkIds = collectChunkIds(List.of(), verbatim);
            recordCall(param, null, usedChunkIds, true);
            return buildResponse(true, "", List.of(), verbatimRefs, usedChunkIds);
        }

        // 4) 게이트 확인(빈 부재 = OFF/키 미주입).
        if (!llmAnswerClient.isEnabled()) {
            throw new ApiException(AiErrorCode.AI_503_001);
        }
        // 4-1) 회사 월간 AI 토큰 쿼터 게이트(플랫폼-AI-토큰쿼터 §2-4) — 소진 시 AI_429_001.
        aiQuotaService.checkOrThrow(param.gvCmpnyCd());

        // 5) recompose 컨텍스트 조립(★verbatim 미포함) → LLM 호출.
        Map<Integer, RagHit> markerToHit = new LinkedHashMap<>();
        Map<String, RagHit> idToHit = new LinkedHashMap<>();
        String userPrompt = buildContextPrompt(param.query(), recompose, markerToHit, idToHit);
        LlmRawResponse raw = llmAnswerClient.answer(SYSTEM_PROMPT, userPrompt);
        // ★쿼터 사용량 누적 — raw 수신 즉시(후속 refusal/파싱 실패 경로에서도 기록 누락 없음).
        aiQuotaService.record(param.gvCmpnyCd(), raw.inputTokens(), raw.outputTokens());

        // 6) refusal → content 파싱 없이 abstain.
        if (raw.refusal()) {
            log.info("RAG 답변 abstain - LLM refusal");
            List<String> usedChunkIds = collectChunkIds(List.of(), verbatim);
            recordCall(param, raw, usedChunkIds, true);
            return buildResponse(true, "", List.of(), verbatimRefs, usedChunkIds);
        }

        // 7) 구조화 JSON 파싱.
        AnswerResult result = parseAnswer(raw.combinedText());

        // 8) LLM 이 abstain 판정.
        if (result.abstained()) {
            log.info("RAG 답변 abstain - LLM abstained=true");
            List<String> usedChunkIds = collectChunkIds(List.of(), verbatim);
            recordCall(param, raw, usedChunkIds, true);
            return buildResponse(true, "", List.of(), verbatimRefs, usedChunkIds);
        }

        // 9) ★citation 검증: chunkId 가 실제 recompose 집합에 있는 것만 채택(환각 인용 차단).
        List<Citation> citations = validateCitations(result, idToHit);
        List<RagHit> citedHits = new ArrayList<>();
        for (Citation c : citations) {
            citedHits.add(idToHit.get(c.getChunkId()));
        }
        List<String> usedChunkIds = collectChunkIds(citedHits, verbatim);

        // 10) 감사·과금 로깅(best-effort) 후 응답.
        recordCall(param, raw, usedChunkIds, false);
        log.info("RAG 답변 완료 - citations={}건, verbatim={}건", citations.size(), verbatimRefs.size());
        return buildResponse(false, result.answer(), citations, verbatimRefs, usedChunkIds);
    }

    // ------------------------------------------------------------------
    // 프롬프트 조립 (★recompose 만)
    // ------------------------------------------------------------------

    /**
     * recompose 청크로 컨텍스트 프롬프트 조립. marker↔hit / chunkId↔hit 매핑을 채운다.
     * ★verbatim 은 인자로 받지 않으므로 콘텐츠가 이 문자열에 절대 포함되지 않는다.
     */
    private String buildContextPrompt(String query, List<RagHit> recompose,
                                      Map<Integer, RagHit> markerToHit, Map<String, RagHit> idToHit) {
        StringBuilder sb = new StringBuilder();
        sb.append("[근거 청크]\n");
        int n = 1;
        for (RagHit h : recompose) {
            markerToHit.put(n, h);
            idToHit.put(h.getChunkId(), h);
            sb.append('[').append(n).append("] (chunkId: ").append(nz(h.getChunkId()))
              .append(" / 출처: ").append(nz(h.getSourceName()))
              .append(" / 신뢰등급: ").append(nz(h.getDataReliability())).append(")\n");
            sb.append(nz(h.getContent())).append("\n\n");
            n++;
        }
        sb.append("[질문] ").append(query);
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 파싱 / 검증
    // ------------------------------------------------------------------

    /** 결합 텍스트에서 JSON 객체 추출 후 AnswerResult 파싱. 실패 → AI_502_004. */
    private AnswerResult parseAnswer(String combinedText) {
        String json = extractJsonObject(combinedText);
        if (json == null) {
            log.error("LLM 응답에서 JSON 객체를 찾지 못함");
            throw new ApiException(AiErrorCode.AI_502_004);
        }
        try {
            return objectMapper.readValue(json, AnswerResult.class);
        } catch (Exception e) {
            log.error("LLM 응답 JSON 파싱 실패 - 원인={}", e.getMessage());
            throw new ApiException(AiErrorCode.AI_502_004);
        }
    }

    /** 코드펜스 제거 후 첫 '{'~마지막 '}' 구간을 JSON 으로 추출. 없으면 null. */
    private String extractJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < 0 || end < start) {
            return null;
        }
        return text.substring(start, end + 1);
    }

    /** LLM citations 중 recompose 집합에 실재하는 chunkId 만 채택하고 서버 메타로 enrich(중복 marker 제거). */
    private List<Citation> validateCitations(AnswerResult result, Map<String, RagHit> idToHit) {
        List<Citation> out = new ArrayList<>();
        if (result.citations() == null) {
            return out;
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (AnswerResult.CitationOut c : result.citations()) {
            if (c == null || c.chunkId() == null) {
                continue;
            }
            RagHit hit = idToHit.get(c.chunkId());
            if (hit == null) {
                // 환각 인용(컨텍스트에 없는 chunkId) → 폐기.
                log.warn("환각 인용 폐기 - chunkId={}", c.chunkId());
                continue;
            }
            if (!seen.add(c.chunkId())) {
                continue;   // 동일 청크 중복 인용 제거
            }
            out.add(Citation.builder()
                .marker(c.marker())
                .chunkId(hit.getChunkId())
                .sourceName(hit.getSourceName())
                .sourceLocator(hit.getSourceLocator())
                .dataReliability(hit.getDataReliability())
                .build());
        }
        return out;
    }

    // ------------------------------------------------------------------
    // 응답 조립 / verbatim
    // ------------------------------------------------------------------

    private List<VerbatimReference> toVerbatimReferences(List<RagHit> verbatim) {
        List<VerbatimReference> out = new ArrayList<>();
        for (RagHit h : verbatim) {
            out.add(VerbatimReference.builder()
                .chunkId(h.getChunkId())
                .sourceName(h.getSourceName())
                .dataReliability(h.getDataReliability())
                .content(h.getContent())
                .sourceLocator(h.getSourceLocator())
                .build());
        }
        return out;
    }

    /** 인용된 recompose 청크 + verbatim 청크의 ID 를 순서 보존·중복 제거로 수집. */
    private List<String> collectChunkIds(List<RagHit> citedHits, List<RagHit> verbatim) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (RagHit h : citedHits) {
            if (h != null && h.getChunkId() != null) {
                ids.add(h.getChunkId());
            }
        }
        for (RagHit h : verbatim) {
            if (h != null && h.getChunkId() != null) {
                ids.add(h.getChunkId());
            }
        }
        return new ArrayList<>(ids);
    }

    private AnswerResponse buildResponse(boolean abstained, String answer,
                                         List<Citation> citations,
                                         List<VerbatimReference> verbatimRefs,
                                         List<String> usedChunkIds) {
        return AnswerResponse.builder()
            .abstained(abstained)
            .answer(answer)
            .citations(citations)
            .verbatimReferences(verbatimRefs)
            .usedChunkIds(usedChunkIds)
            .disclaimer(DISCLAIMER)
            .build();
    }

    // ------------------------------------------------------------------
    // 감사·과금 (best-effort)
    // ------------------------------------------------------------------

    /** 감사 로그 기록. raw==null(LLM 미호출) 이면 토큰/비용 0. 실패해도 응답은 성공(경고 로그). */
    private void recordCall(AnswerParam param, LlmRawResponse raw,
                            List<String> usedChunkIds, boolean abstained) {
        try {
            long input = (raw == null) ? 0L : raw.inputTokens();
            long output = (raw == null) ? 0L : raw.outputTokens();
            long cacheRead = (raw == null) ? 0L : raw.cacheReadTokens();
            long cacheCreation = (raw == null) ? 0L : raw.cacheCreationTokens();
            String model = aiProperties.getLlm().getModel();
            BigDecimal cost = computeCost(model, input, output, cacheRead, cacheCreation);

            AiCallLog logRow = new AiCallLog(
                UUID.randomUUID().toString(),
                param.gvUserCd(),
                param.gvCmpnyCd(),
                ENDPOINT,
                model,
                param.query().length(),          // ★원문 미저장(길이만·해시도 미저장 = PII 역추적 리스크 제거)
                input, output, cacheRead, cacheCreation,
                cost,
                usedChunkIds,
                abstained);
            aiCallRepository.save(logRow);
        } catch (Exception e) {
            // 감사 로깅 실패가 답변 성공을 훼손하지 않도록 삼킨다.
            log.warn("tb_ai_call 감사 로깅 실패(응답은 정상 반환) - 원인={}", e.getMessage());
        }
    }

    /**
     * HCX 과금 계산: 설정 단가(1k 토큰당, 기본 0) 기반. 캐시 단가 없음(cacheRead/cacheCreation 는 항상 0 → 무시).
     * COST 컬럼은 공급자 통화(KRW) 의미이며 단가 미설정(0) 시 0 으로 저장된다.
     * model 인자는 감사 로그 스키마 호환을 위해 시그니처만 유지(HCX 는 단일 단가라 분기 없음).
     */
    private BigDecimal computeCost(String model, long input, long output, long cacheRead, long cacheCreation) {
        double inRate = aiProperties.getLlm().getCostPer1kInput();    // 입력 단가/1k
        double outRate = aiProperties.getLlm().getCostPer1kOutput();  // 출력 단가/1k
        double cost = (input / 1_000d) * inRate
                    + (output / 1_000d) * outRate;
        return new BigDecimal(cost, MathContext.DECIMAL64).setScale(5, RoundingMode.HALF_UP);
    }

    /** null → 빈 문자열. */
    private String nz(String v) {
        return (v == null) ? "" : v;
    }
}
