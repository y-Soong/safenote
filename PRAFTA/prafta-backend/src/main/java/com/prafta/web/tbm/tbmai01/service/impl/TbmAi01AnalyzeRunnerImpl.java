package com.prafta.web.tbm.tbmai01.service.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.app.ai.ai01.application.model.AiCallLog;
import com.prafta.app.ai.ai01.client.ImagePart;
import com.prafta.app.ai.ai01.client.LlmAnswerClient;
import com.prafta.app.ai.ai01.client.LlmRawResponse;
import com.prafta.app.ai.ai01.repository.AiCallRepository;
import com.prafta.common.cmm.aiquota.service.AiQuotaService;
import com.prafta.common.cmm.file.application.model.ImageBytesResult;
import com.prafta.common.cmm.file.application.query.FileReadQuery;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.config.AiProperties;
import com.prafta.web.tbm.tbmai01.application.model.TbmAiAnalyzeTarget;
import com.prafta.web.tbm.tbmai01.dto.response.TbmAiChatTurn;
import com.prafta.web.tbm.tbmai01.mapper.TbmAi01Mapper;
import com.prafta.web.tbm.tbmai01.service.TbmAi01AnalyzeRunner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TBM 세부항목 비동기 VLM 분석 러너 구현.
 *
 * <p>★트랜잭션 경계: {@code runAsync}/{@code analyzeOne} 에 {@code @Transactional} 을 부여하지 않는다.
 *    항목별 mapper UPDATE 는 단발 autocommit(UploadJob 관례)으로 각 항목이 독립적으로 즉시 가시화되고,
 *    한 항목 실패가 앞선 성공을 롤백하지 않는다. 감사({@code recordCall})는 2차 DS(pgvector)로 별도 자율 기록.
 * <p>★이미지 상한: HCX 요청당 이미지 ≤5. PDF 는 {@code maxPages=min(pdfMaxPages, MAX_VLM_IMAGES)} 로 클램프.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TbmAi01AnalyzeRunnerImpl implements TbmAi01AnalyzeRunner {

    private final TbmAi01Mapper tbmAi01Mapper;
    private final FileService fileService;
    private final LlmAnswerClient llmAnswerClient;
    private final AiCallRepository aiCallRepository;
    private final AiQuotaService aiQuotaService;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    /** HCX 요청당 이미지 상한(HCX-005 제약). PDF 렌더 페이지 클램프 기준. */
    private static final int MAX_VLM_IMAGES = 5;

    private static final String TYPE_IMAGE = "01";
    private static final String TYPE_PDF = "04";

    private static final String ROLE_ASSISTANT = "assistant";

    /**
     * 항목 분석(analyzeOne) 시스템 프롬프트 — 첨부(이미지/PDF 페이지)에서 보이는 것만 근거로 TBM 요지 서술.
     */
    private static final String TBM_ANALYZE_SYSTEM_PROMPT =
          "당신은 산업안전 TBM 교육 보조자다. 첨부된 교육자료(이미지 또는 PDF 페이지)에서 실제로 보이는 내용만\n"
        + "근거로, TBM 교육에 활용할 핵심 안전 요지를 간결한 한국어 서술로 정리한다.\n"
        + "1. 보이지 않는 사실을 지어내지 않는다.\n"
        + "2. 유해·위험요인과 안전수칙 관점으로 서술한다.\n"
        + "3. 머리말/맺음말/JSON 없이 서술 텍스트만 반환한다.";

    @Override
    @Async
    public void runAsync(List<TbmAiAnalyzeTarget> targets, String cmpnyCd, String userCd,
                         String endpoint, String adminNote) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        for (TbmAiAnalyzeTarget t : targets) {
            if (t == null) {
                continue;
            }
            try {
                analyzeOne(t, cmpnyCd, userCd, endpoint, adminNote);
            } catch (Exception e) {
                // ★한 항목 실패가 다른 항목을 막지 않는다. FAILED 로 확정 후 계속.
                log.error("TBM AI 항목 분석 실패 - mtrlItemCd={}, 원인={}", t.mtrlItemCd(), e.getMessage());
                safeFail(t.mtrlItemCd(), cmpnyCd, userCd);
            }
        }
    }

    /** 항목 1건 분석: 게이트 → 첨부 로드(≤5) → VLM → DRAFT/FAILED 저장 + 감사. */
    private void analyzeOne(TbmAiAnalyzeTarget t, String cmpnyCd, String userCd,
                            String endpoint, String adminNote) {
        // 게이트 OFF: 비동기라 예외 전파 불가 → 상태(FAILED)로만 표현.
        if (!llmAnswerClient.isEnabled()) {
            log.warn("TBM AI 분석 게이트 OFF - mtrlItemCd={} FAILED 처리", t.mtrlItemCd());
            safeFail(t.mtrlItemCd(), cmpnyCd, userCd);
            return;
        }
        // 회사 월간 AI 토큰 쿼터 소진: 비동기 — throw 금지, FAILED 상태로만 표현(게이트 OFF 와 동일 패턴).
        //   자동 큐잉(enqueueOnSave)이 스킵을 놓쳐도 이 러너 가드가 항목별 최종 방어선이다.
        if (aiQuotaService.isExceeded(cmpnyCd)) {
            log.warn("TBM AI 분석 차단 - AI 토큰 쿼터 소진 - cmpnyCd={}, mtrlItemCd={} FAILED 처리",
                cmpnyCd, t.mtrlItemCd());
            safeFail(t.mtrlItemCd(), cmpnyCd, userCd);
            return;
        }

        // 첨부 로드(타입 분기, ≤5장 클램프). 대상 없음 → FAILED.
        List<ImagePart> images = loadImages(cmpnyCd, t);
        if (images.isEmpty()) {
            log.warn("TBM AI 분석 첨부 없음 - mtrlItemCd={}, type={} FAILED 처리",
                t.mtrlItemCd(), t.mtrlItemType());
            safeFail(t.mtrlItemCd(), cmpnyCd, userCd);
            return;
        }

        String userPrompt = buildUserPrompt(adminNote);
        LlmRawResponse raw = llmAnswerClient.answerWithImages(TBM_ANALYZE_SYSTEM_PROMPT, userPrompt, images);
        // ★쿼터 사용량 누적 — raw 수신 즉시(초안 유효성/FAILED 전이와 무관).
        aiQuotaService.record(cmpnyCd, raw.inputTokens(), raw.outputTokens());

        // 결과 파싱: 빈 문자열 → 초안 없음 → FAILED.
        String draft = raw.refusal() ? "" : safeTrim(raw.combinedText());
        if (!StringUtils.hasText(draft)) {
            log.warn("TBM AI 분석 초안 비어 있음 - mtrlItemCd={} FAILED 처리", t.mtrlItemCd());
            safeFail(t.mtrlItemCd(), cmpnyCd, userCd);
            recordCall(userCd, cmpnyCd, endpoint, raw, userPrompt.length());
            return;
        }

        // 초안을 assistant 첫 발화로 시드 → chat-item 진입 시 팝업이 초안을 첫 메시지로 렌더.
        List<TbmAiChatTurn> seed = new ArrayList<>();
        seed.add(new TbmAiChatTurn(ROLE_ASSISTANT, draft));
        tbmAi01Mapper.updateItemDraft(t.mtrlItemCd(), draft, toJson(seed), cmpnyCd, userCd);

        recordCall(userCd, cmpnyCd, endpoint, raw, userPrompt.length());
        log.info("TBM AI 항목 분석 완료 - mtrlItemCd={}, type={}, images={}",
            t.mtrlItemCd(), t.mtrlItemType(), images.size());
    }

    /** 타입 분기 첨부 로드(이미지 1장 / PDF ≤5페이지). 대상 없으면 빈 목록. */
    private List<ImagePart> loadImages(String cmpnyCd, TbmAiAnalyzeTarget t) {
        FileReadQuery query = new FileReadQuery(cmpnyCd, t.fileMgmtCd());
        if (TYPE_IMAGE.equals(t.mtrlItemType())) {
            ImageBytesResult img = fileService.loadImageBytes(query);
            if (img == null || img.data() == null || img.data().length == 0) {
                return List.of();
            }
            return List.of(new ImagePart(base64(img.data()), img.mediaType()));
        }
        if (TYPE_PDF.equals(t.mtrlItemType())) {
            int cap = Math.min(aiProperties.getTbm().getPdfMaxPages(), MAX_VLM_IMAGES);
            List<ImageBytesResult> pages =
                fileService.loadPdfPageImages(query, aiProperties.getTbm().getPdfPageStride(), cap);
            if (pages == null || pages.isEmpty()) {
                return List.of();
            }
            List<ImagePart> out = new ArrayList<>();
            for (ImageBytesResult p : pages) {
                if (p == null || p.data() == null || p.data().length == 0) {
                    continue;
                }
                out.add(new ImagePart(base64(p.data()), p.mediaType()));
                if (out.size() >= MAX_VLM_IMAGES) {
                    break;
                }
            }
            return out;
        }
        // 분석 대상 타입이 아님(선행 필터로 도달 불가하나 방어).
        return List.of();
    }

    /** user 프롬프트: 분석 지시 + 관리자 의견(재분석 시, 있으면). */
    private String buildUserPrompt(String adminNote) {
        StringBuilder sb = new StringBuilder();
        sb.append("첨부된 교육자료를 분석해 TBM 교육에 활용할 핵심 안전 요지를 정리해 주세요.");
        if (StringUtils.hasText(adminNote)) {
            sb.append("\n\n[관리자 의견]\n").append(adminNote.trim());
        }
        return sb.toString();
    }

    /** 실패 확정(best-effort — 감사 실패가 상태 전이를 막지 않도록 예외 삼킴). */
    private void safeFail(String mtrlItemCd, String cmpnyCd, String userCd) {
        try {
            tbmAi01Mapper.updateItemFailed(mtrlItemCd, cmpnyCd, userCd);
        } catch (Exception ignore) {
            // 마지막 UPDATE 도 실패하면 application log 만 남기고 종료.
        }
    }

    // ==================================================================
    // 내부 헬퍼 — 감사·과금(best-effort, 2차 DS)
    // ==================================================================

    /** 감사 로그 기록. raw==null 이면 토큰/비용 0. 실패해도 분석은 성공(경고 로그). */
    private void recordCall(String userCd, String cmpnyCd, String endpoint, LlmRawResponse raw, int queryLen) {
        try {
            long input = (raw == null) ? 0L : raw.inputTokens();
            long output = (raw == null) ? 0L : raw.outputTokens();
            long cacheRead = (raw == null) ? 0L : raw.cacheReadTokens();
            long cacheCreation = (raw == null) ? 0L : raw.cacheCreationTokens();
            String model = aiProperties.getLlm().getModel();
            BigDecimal cost = computeCost(input, output);

            AiCallLog logRow = new AiCallLog(
                UUID.randomUUID().toString(),
                userCd,
                cmpnyCd,
                endpoint,
                model,
                queryLen,               // ★원문 미저장(길이만)
                input, output, cacheRead, cacheCreation,
                cost,
                List.of(),              // RAG 미사용 → 사용 청크 없음
                false);
            aiCallRepository.save(logRow);
        } catch (Exception e) {
            log.warn("tb_ai_call 감사 로깅 실패(분석은 정상) - 원인={}", e.getMessage());
        }
    }

    /** HCX 과금 계산: 설정 단가(1k 토큰당) 기반. 캐시 단가 없음. */
    private BigDecimal computeCost(long input, long output) {
        double inRate = aiProperties.getLlm().getCostPer1kInput();
        double outRate = aiProperties.getLlm().getCostPer1kOutput();
        double cost = (input / 1_000d) * inRate + (output / 1_000d) * outRate;
        return new BigDecimal(cost, MathContext.DECIMAL64).setScale(5, RoundingMode.HALF_UP);
    }

    private String base64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private String safeTrim(String v) {
        return (v == null) ? "" : v.trim();
    }

    /** 객체 → JSON 문자열. 직렬화 실패 시 빈 배열. */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            log.warn("JSON 직렬화 실패 - 빈 배열로 대체", e);
            return "[]";
        }
    }
}
