package com.prafta.web.tbm.tbmai01.service.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.app.ai.ai01.application.model.AiCallLog;
import com.prafta.app.ai.ai01.client.HcxTurn;
import com.prafta.app.ai.ai01.client.ImagePart;
import com.prafta.app.ai.ai01.client.LlmAnswerClient;
import com.prafta.app.ai.ai01.client.LlmRawResponse;
import com.prafta.app.ai.ai01.repository.AiCallRepository;
import com.prafta.common.cmm.file.application.model.ImageBytesResult;
import com.prafta.common.cmm.file.application.query.FileReadQuery;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.config.AiProperties;
import com.prafta.common.error.ai.AiErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.FileUrlSigner;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbmai01.application.model.TbmAiAnalyzeTarget;
import com.prafta.web.tbm.tbmai01.application.model.TbmAiItemRow;
import com.prafta.web.tbm.tbmai01.application.model.TbmAiItemStatusRow;
import com.prafta.web.tbm.tbmai01.application.model.TbmAiMtrlScope;
import com.prafta.web.tbm.tbmai01.application.model.TbmAiWorklistRow;
import com.prafta.web.tbm.tbmai01.application.param.TbmAiParam;
import com.prafta.web.tbm.tbmai01.application.param.TbmAiWorklistParam;
import com.prafta.web.tbm.tbmai01.application.query.TbmAiWorklistQuery;
import com.prafta.web.tbm.tbmai01.dto.response.TbmAiAnalysisStatusResponse;
import com.prafta.web.tbm.tbmai01.dto.response.TbmAiChatTurn;
import com.prafta.web.tbm.tbmai01.dto.response.TbmAiItemResponse;
import com.prafta.web.tbm.tbmai01.dto.response.TbmAiWorklistResponse;
import com.prafta.web.tbm.tbmai01.mapper.TbmAi01Mapper;
import com.prafta.web.tbm.tbmai01.service.TbmAi01AnalyzeRunner;
import com.prafta.web.tbm.tbmai01.service.TbmAi01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TBM 세부항목 AI 분석·확정 서비스 구현.
 *
 * <p>★riskai01 과의 결정적 차이: 별도 도출 테이블 없이 AI 상태·초안·확정서술·대화이력을
 *    {@code tb_tbm_edu_mtrl_item} 신규 컬럼에 직접 저장한다. 모든 항목 UPDATE/SELECT 는
 *    항목 테이블 자신의 {@code CMPNY_CD}(JWT 도출 gvCmpnyCd)로 회사 스코프(IDOR)를 강제한다
 *    — 항목/자료 코드는 회사별 채번이라 전역 유일하지 않으므로 마스터 조인만으로는 격리가 안 된다.
 * <p>★비동기: 초기/재분석 트리거는 {@code markItemsAnalyzing}(autocommit)로 ANALYZING 선커밋 후
 *    별도 러너 빈({@link TbmAi01AnalyzeRunner}, 프록시 경유)을 호출한다. 본 서비스 메서드에는
 *    {@code @Transactional} 을 부여하지 않는다(ANALYZING 이 async 시작 전 커밋되도록).
 * <p>★RAG 미사용: 근거=첨부(VLM)+관리자 입력. {@code Ai01Service.search} 호출 없음.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TbmAi01ServiceImpl implements TbmAi01Service {

    private final TbmAi01Mapper tbmAi01Mapper;
    private final TbmAi01AnalyzeRunner analyzeRunner;
    private final FileService fileService;
    private final LlmAnswerClient llmAnswerClient;
    private final AiCallRepository aiCallRepository;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final FileUrlSigner fileUrlSigner;   // 파일 서빙 서명 URL 발급(워크리스트 파일 항목)

    /** HCX 요청당 이미지 상한(HCX-005 제약). PDF 렌더 페이지 클램프 기준. */
    private static final int MAX_VLM_IMAGES = 5;

    /** AI_CONFIRM_DESC varchar(2000) 절단 방어. */
    private static final int MAX_CONFIRM_DESC_LEN = 2000;

    /** 대화 메시지 길이 클램프(과대입력 방어). */
    private static final int MAX_USER_MSG_LEN = 2000;

    private static final String TYPE_IMAGE = "01";
    private static final String TYPE_PDF = "04";

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private static final String ENDPOINT_ANALYZE = "tbmai01/analyze";
    private static final String ENDPOINT_CHAT = "tbmai01/chat-item";
    private static final String ENDPOINT_REANALYZE = "tbmai01/reanalyze";

    private static final String DISCLAIMER =
        "본 내용은 AI 가 제시한 참고 초안이며, 최종 판단은 담당자가 수행합니다.";

    /** 대화형 보완(chat-item) 시스템 프롬프트. */
    private static final String TBM_CHAT_SYSTEM_PROMPT =
          "당신은 산업안전 TBM 교육 보조자다. 첨부된 교육자료(이미지 또는 PDF 페이지)에 대한 관리자의\n"
        + "보완·정정 지시를 반영해 TBM 교육 요지를 수정한다.\n"
        + "1. 첨부에서 실제로 보이는 내용만 근거로 한다(보이지 않는 사실을 지어내지 않는다).\n"
        + "2. 이전 대화 맥락을 이어 일관되게 답한다.\n"
        + "3. 머리말/맺음말/JSON 없이 서술 텍스트만 반환한다.";

    // ==================================================================
    // ① analyze-items — 초기 큐잉
    // ==================================================================

    @Override
    public TbmAiAnalysisStatusResponse analyzeItems(TbmAiParam param) {
        log.info("TBM AI 항목 분석 큐잉 진입 - cmpnyCd={}, mtrlCd={}", param.gvCmpnyCd(), param.mtrlCd());

        if (!StringUtils.hasText(param.mtrlCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // 회사 미소유 자료 → 존재 비노출 404. 소유 확인과 사업장 스코프 도출을 한 번에.
        TbmAiMtrlScope scope = tbmAi01Mapper.selectMtrlSiteScope(param.mtrlCd(), param.gvCmpnyCd());
        if (scope == null) {
            throw new ApiException(AiErrorCode.AI_404_002);
        }
        // ★사업장 격리(쓰기): 동일 회사 타 사업장 자료 큐잉/변조 차단(T2 generate 게이트와 대칭).
        assertSiteAccess(scope.siteCd(), param);
        // 게이트 OFF → 사전거부(큐잉 후 전부 FAILED 되는 낭비 방지).
        if (!llmAnswerClient.isEnabled()) {
            throw new ApiException(AiErrorCode.AI_503_001);
        }

        List<TbmAiAnalyzeTarget> targets =
            tbmAi01Mapper.selectAnalyzeTargets(param.mtrlCd(), param.gvCmpnyCd());
        if (targets != null && !targets.isEmpty()) {
            // ★ANALYZING 선커밋(autocommit) 후 비동기 트리거(프록시 경유, 즉시 반환).
            tbmAi01Mapper.markItemsAnalyzing(param.mtrlCd(), param.gvCmpnyCd(), param.gvUserCd());
            analyzeRunner.runAsync(targets, param.gvCmpnyCd(), param.gvUserCd(), ENDPOINT_ANALYZE, null);
            log.info("TBM AI 항목 분석 트리거 - mtrlCd={}, 대상={}건", param.mtrlCd(), targets.size());
        } else {
            log.info("TBM AI 항목 분석 대상 없음 - mtrlCd={}", param.mtrlCd());
        }

        return buildStatusResponse(param.mtrlCd(), param.gvCmpnyCd());
    }

    // ==================================================================
    // ①-2 enqueueOnSave — 교육자료 저장 직후 자동 큐잉(best-effort)
    // ==================================================================

    /**
     * ★REQUIRES_NEW 필수. 이 메서드는 저장 트랜잭션의 {@code afterCommit} 콜백에서 호출된다.
     * 그 시점의 스레드에는 이미 커밋된 SqlSession/커넥션이 아직 바인딩되어 있어, 새 트랜잭션을
     * 열지 않으면 {@code markItemsAnalyzing} UPDATE 가 커밋되지 못하고 세션 정리 시 유실된다
     * (항목이 ANALYZING 으로 전이되지 않아 배지가 "분석대기"에 머문다).
     * 새 트랜잭션으로 분리해 UPDATE 를 확정한 뒤 비동기 분석을 트리거한다.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueOnSave(String mtrlCd, String cmpnyCd, String userCd, String authCd) {
        try {
            // 필수 스코프 누락 → 조용히 return.
            if (!StringUtils.hasText(mtrlCd) || !StringUtils.hasText(cmpnyCd)) {
                return;
            }
            TbmAiMtrlScope scope = tbmAi01Mapper.selectMtrlSiteScope(mtrlCd, cmpnyCd);
            if (scope == null) {
                log.info("TBM AI 자동 큐잉 건너뜀 - 회사 미소유/미존재 자료 - mtrlCd={}", mtrlCd);
                return;
            }
            // ★사업장 인가: 저장 경로(특히 잠금자료 AI 지정 경로)는 회사 스코프까지만 검증하므로,
            //   여기서 analyzeItems 와 동일한 사업장 격리를 다시 강제한다. 이 게이트가 없으면 사업장 A
            //   권한자가 사업장 B·회사공통 자료를 저장해 유료 VLM 분석과 초안 덮어쓰기를 유발할 수 있다.
            //   afterCommit 흐름을 깨지 않도록 예외 대신 조용히 스킵한다(저장 자체는 이미 확정).
            if (!hasSiteAccess(scope.siteCd(), cmpnyCd, userCd, authCd)) {
                log.warn("TBM AI 자동 큐잉 차단 - 사업장 접근 권한 없음 - mtrlCd={}, targetSite={}, userCd={}",
                    mtrlCd, scope.siteCd(), userCd);
                return;
            }
            // 게이트 OFF → 조용히 스킵(저장은 이미 성공). analyzeItems 의 AI_503_001 throw 는 여기서 쓰지 않는다.
            if (!llmAnswerClient.isEnabled()) {
                log.info("AI 게이트 OFF — 자동 큐잉 건너뜀 - mtrlCd={}", mtrlCd);
                return;
            }
            // ★자동 큐잉은 DRAFT 를 제외한다(selectAutoAnalyzeTargets). 검토 중인 초안·대화 이력을
            //   무관한 저장 한 번으로 덮어쓰지 않기 위함. DRAFT 재분석은 reanalyze EP 로만.
            List<TbmAiAnalyzeTarget> targets = tbmAi01Mapper.selectAutoAnalyzeTargets(mtrlCd, cmpnyCd);
            if (targets == null || targets.isEmpty()) {
                log.info("TBM AI 자동 큐잉 대상 없음 - mtrlCd={}", mtrlCd);
                return;
            }
            // ANALYZING 전이는 이 REQUIRES_NEW 트랜잭션 커밋 시 확정된다. 비동기 분석은 이미지 로드·LLM 호출로
            //   수 초가 걸리므로, 그 사이 커밋이 끝나 행 잠금 경합은 사실상 발생하지 않는다.
            //   ★전이된 행이 0건이면(동시 저장 경합으로 다른 트랜잭션이 이미 큐잉) 러너를 띄우지 않는다 —
            //   중복 VLM 호출(유료) 방지.
            int queued = tbmAi01Mapper.markItemsAnalyzingAuto(mtrlCd, cmpnyCd, userCd);
            if (queued == 0) {
                log.info("TBM AI 자동 큐잉 스킵 - 이미 큐잉된 항목(전이 0건) - mtrlCd={}", mtrlCd);
                return;
            }
            analyzeRunner.runAsync(targets, cmpnyCd, userCd, ENDPOINT_ANALYZE, null);
            log.info("TBM AI 자동 큐잉 트리거 - mtrlCd={}, 대상={}건", mtrlCd, targets.size());
        } catch (Exception e) {
            // ★저장 흐름 보호: 어떤 예외도 밖으로 던지지 않는다(로그만).
            log.warn("TBM AI 자동 큐잉 실패(저장은 정상 반영) - mtrlCd={}, 원인={}", mtrlCd, e.getMessage());
        }
    }

    // ==================================================================
    // ② analysis-status — 조회(읽기전용, LLM 미호출)
    // ==================================================================

    @Override
    public TbmAiAnalysisStatusResponse analysisStatus(TbmAiParam param) {
        log.info("TBM AI 분석상태 조회 진입 - cmpnyCd={}, mtrlCd={}", param.gvCmpnyCd(), param.mtrlCd());

        if (!StringUtils.hasText(param.mtrlCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        TbmAiMtrlScope scope = tbmAi01Mapper.selectMtrlSiteScope(param.mtrlCd(), param.gvCmpnyCd());
        if (scope == null) {
            throw new ApiException(AiErrorCode.AI_404_002);
        }
        // ★사업장 격리(읽기): 접근 권한 없으면 존재 비노출(AI_404_002)로 수렴(단건 접근 일관성).
        if (!hasSiteAccess(scope.siteCd(), param)) {
            log.warn("TBM AI 분석상태 조회 - 사업장 접근 권한 없음(존재 비노출) - mtrlCd={}, siteCd={}",
                param.mtrlCd(), scope.siteCd());
            throw new ApiException(AiErrorCode.AI_404_002);
        }
        return buildStatusResponse(param.mtrlCd(), param.gvCmpnyCd());
    }

    // ==================================================================
    // ③ chat-item — 대화형 보완(VLM, 캡 없음, 이미지 매요청 재부착)
    // ==================================================================

    @Override
    public TbmAiItemResponse chatItem(TbmAiParam param) {
        log.info("TBM AI 항목 대화 진입 - cmpnyCd={}, mtrlItemCd={}", param.gvCmpnyCd(), param.mtrlItemCd());

        if (!StringUtils.hasText(param.mtrlItemCd()) || !StringUtils.hasText(param.userMessage())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        TbmAiItemRow row = loadItemOr404(param);
        assertSiteAccess(row.siteCd(), param);
        assertAnalyzable(row);

        if (!llmAnswerClient.isEnabled()) {
            throw new ApiException(AiErrorCode.AI_503_001);
        }

        // ★매 요청 디스크 재로드(HCX stateless). null/빈 → 404.
        List<ImagePart> images = buildVlmImagesOr404(param.gvCmpnyCd(), row);

        // 이력 로드 → user 턴 append.
        List<TbmAiChatTurn> turns = new ArrayList<>(parseChatTurns(row.aiChatJson()));
        turns.add(new TbmAiChatTurn(ROLE_USER, clamp(param.userMessage(), MAX_USER_MSG_LEN)));

        // 첫 user 턴에만 이미지(들) 재부착(PDF 다페이지도 첫 user 턴에 일괄, ≤5).
        List<HcxTurn> hcxTurns = toHcxTurns(turns, images);
        LlmRawResponse raw = llmAnswerClient.chat(TBM_CHAT_SYSTEM_PROMPT, hcxTurns);

        // 비-refusal + 유효 텍스트 → assistant 턴 append.
        if (!raw.refusal()) {
            String answer = safeTrim(raw.combinedText());
            if (StringUtils.hasText(answer)) {
                turns.add(new TbmAiChatTurn(ROLE_ASSISTANT, answer));
            }
        }
        // 대화 이력 저장(텍스트만). ★상태는 DRAFT 유지(chat 은 자동확정 아님).
        tbmAi01Mapper.updateItemChatJson(param.mtrlItemCd(), toJson(turns), param.gvCmpnyCd(), param.gvUserCd());

        recordCall(param, ENDPOINT_CHAT, raw, param.userMessage().length());

        TbmAiItemRow after = loadItemOr404(param);
        log.info("TBM AI 항목 대화 완료 - mtrlItemCd={}, refusal={}, turns={}",
            param.mtrlItemCd(), raw.refusal(), turns.size());
        return TbmAiItemResponse.from(after, parseChatTurns(after.aiChatJson()), DISCLAIMER);
    }

    // ==================================================================
    // ④ confirm-item — 확정(멱등, LLM 미호출)
    // ==================================================================

    @Override
    public TbmAiItemResponse confirmItem(TbmAiParam param) {
        log.info("TBM AI 항목 확정 진입 - cmpnyCd={}, mtrlItemCd={}", param.gvCmpnyCd(), param.mtrlItemCd());

        if (!StringUtils.hasText(param.mtrlItemCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        TbmAiItemRow row = loadItemOr404(param);
        assertSiteAccess(row.siteCd(), param);

        // ★확정 텍스트 결정(타입 분기).
        //   - 01 이미지 / 04 PDF(VLM 경로): 요청값 > 최신 assistant 턴 > 초안 순. 최종 공백이면 확정 불가.
        //   - 02 동영상 / 03 유튜브(텍스트 경로, LLM 미호출): confirmText 를 직접 확정. 빈 확정 불허
        //     — 교육안 생성 게이트(tbmai02)가 "확정 = CONFIRMED 且 서술 비공백"이라, 빈 확정은
        //       생성 차단(TBM_409_060)만 유발하므로 확정 시점에 텍스트를 강제한다(2026-07-11 정책 변경).
        String confirmDesc;
        if (isVlmType(row.mtrlItemType())) {
            if (StringUtils.hasText(param.confirmText())) {
                confirmDesc = clamp(param.confirmText(), MAX_CONFIRM_DESC_LEN);
            } else {
                String latest = latestAssistantText(parseChatTurns(row.aiChatJson()));
                confirmDesc = StringUtils.hasText(latest)
                    ? clamp(latest, MAX_CONFIRM_DESC_LEN)
                    : clamp(row.aiDraftText(), MAX_CONFIRM_DESC_LEN);
            }
        } else {
            // 동영상/유튜브: 관리자 설명 텍스트를 그대로 확정.
            confirmDesc = clamp(param.confirmText(), MAX_CONFIRM_DESC_LEN);
        }
        if (!StringUtils.hasText(confirmDesc)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        tbmAi01Mapper.updateItemConfirm(param.mtrlItemCd(), confirmDesc, param.gvCmpnyCd(), param.gvUserCd());

        TbmAiItemRow after = loadItemOr404(param);
        log.info("TBM AI 항목 확정 완료 - mtrlItemCd={}", param.mtrlItemCd());
        return TbmAiItemResponse.from(after, parseChatTurns(after.aiChatJson()), DISCLAIMER);
    }

    // ==================================================================
    // ⑤ reanalyze — 재분석 재진입(캡 없음)
    // ==================================================================

    @Override
    public TbmAiItemResponse reanalyze(TbmAiParam param) {
        log.info("TBM AI 항목 재분석 진입 - cmpnyCd={}, mtrlItemCd={}", param.gvCmpnyCd(), param.mtrlItemCd());

        if (!StringUtils.hasText(param.mtrlItemCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        TbmAiItemRow row = loadItemOr404(param);
        assertSiteAccess(row.siteCd(), param);
        assertAnalyzable(row);
        if (!llmAnswerClient.isEnabled()) {
            throw new ApiException(AiErrorCode.AI_503_001);
        }
        // 첨부 사전검증(없으면 재분석 불가 조기거부).
        buildVlmImagesOr404(param.gvCmpnyCd(), row);

        // ★ANALYZING 선커밋(autocommit) 후 단건 비동기 트리거.
        tbmAi01Mapper.markItemAnalyzing(param.mtrlItemCd(), param.gvCmpnyCd(), param.gvUserCd());
        List<TbmAiAnalyzeTarget> targets = List.of(
            new TbmAiAnalyzeTarget(row.mtrlItemCd(), row.mtrlItemType(), row.fileMgmtCd()));
        analyzeRunner.runAsync(targets, param.gvCmpnyCd(), param.gvUserCd(),
            ENDPOINT_REANALYZE, clamp(param.adminNote(), MAX_USER_MSG_LEN));

        TbmAiItemRow after = loadItemOr404(param);
        log.info("TBM AI 항목 재분석 트리거 - mtrlItemCd={}", param.mtrlItemCd());
        return TbmAiItemResponse.from(after, parseChatTurns(after.aiChatJson()), DISCLAIMER);
    }

    // ==================================================================
    // ⑥ manual-confirm — 실패 수기 확정(LLM 미호출)
    // ==================================================================

    @Override
    public TbmAiItemResponse manualConfirm(TbmAiParam param) {
        log.info("TBM AI 항목 수기확정 진입 - cmpnyCd={}, mtrlItemCd={}", param.gvCmpnyCd(), param.mtrlItemCd());

        if (!StringUtils.hasText(param.mtrlItemCd()) || !StringUtils.hasText(param.manualText())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        TbmAiItemRow row = loadItemOr404(param);
        assertSiteAccess(row.siteCd(), param);
        assertAnalyzable(row);

        tbmAi01Mapper.updateItemConfirm(param.mtrlItemCd(),
            clamp(param.manualText(), MAX_CONFIRM_DESC_LEN), param.gvCmpnyCd(), param.gvUserCd());

        TbmAiItemRow after = loadItemOr404(param);
        log.info("TBM AI 항목 수기확정 완료 - mtrlItemCd={}", param.mtrlItemCd());
        return TbmAiItemResponse.from(after, parseChatTurns(after.aiChatJson()), DISCLAIMER);
    }

    // ==================================================================
    // ⑦ analysis-worklist — AI 분석 워크리스트(읽기전용, LLM 미호출)
    // ==================================================================

    @Override
    public TbmAiWorklistResponse worklist(TbmAiWorklistParam param) {
        log.info("TBM AI 분석 워크리스트 진입 - cmpnyCd={}, page={}, size={}",
            param.gvCmpnyCd(), param.page(), param.size());

        // 스코프 플래그·offset/limit 산출(사업장 격리는 SQL WHERE 로 assertSiteAccess 동치 처리).
        TbmAiWorklistQuery q = TbmAiWorklistQuery.from(param);

        int total = tbmAi01Mapper.countAnalysisWorklist(q);
        List<TbmAiWorklistRow> rows = tbmAi01Mapper.selectAnalysisWorklist(q);

        List<TbmAiWorklistResponse.Item> items = new ArrayList<>();
        if (rows != null) {
            for (TbmAiWorklistRow r : rows) {
                if (r == null) {
                    continue;
                }
                // 파일형 항목(FILE_PATH+FILE_MGMT_CD 존재)만 서명 절대 URL 발급(Tbm01 applyFileUrls 동일 조립).
                String fileUrl = null;
                if (StringUtils.hasText(r.filePath()) && StringUtils.hasText(r.fileMgmtCd())) {
                    String relPath = r.filePath() + "/" + r.fileMgmtCd()
                        + (r.fileExt() != null ? r.fileExt() : "");
                    fileUrl = fileUrlSigner.sign(relPath, param.gvCmpnyCd());
                }
                items.add(new TbmAiWorklistResponse.Item(
                    r.mtrlCd()
                    , r.title()
                    , r.siteCd()
                    , r.mtrlItemCd()
                    , r.mtrlItemType()
                    , r.mtrlDesc()
                    , r.aiAnalyzeYn()
                    , r.aiStatus()
                    , r.hasConfirmDesc()
                    , r.fileMgmtCd()
                    , r.fileNm()
                    , r.filePath()
                    , r.fileExt()
                    , fileUrl
                    , r.url()
                    , r.thumbFileMgmtCd()
                    , r.durationSec()
                    , r.aiAnalyzedAt()
                ));
            }
        }
        log.info("TBM AI 분석 워크리스트 완료 - total={}, 반환={}건", total, items.size());
        return TbmAiWorklistResponse.of(items, total, param.page(), param.size());
    }

    // ==================================================================
    // 내부 헬퍼 — 스코프/존재/검증
    // ==================================================================

    /** 항목 AI행 로드(회사 스코프). NULL 이면 미소유/미존재 → AI_404_002(존재 비노출). */
    private TbmAiItemRow loadItemOr404(TbmAiParam param) {
        TbmAiItemRow row = tbmAi01Mapper.selectItemAiRow(param.mtrlItemCd(), param.gvCmpnyCd());
        if (row == null) {
            throw new ApiException(AiErrorCode.AI_404_002);
        }
        return row;
    }

    /** VLM 분석 대상 타입(01 이미지 / 04 PDF) 여부. */
    private boolean isVlmType(String mtrlItemType) {
        return TYPE_IMAGE.equals(mtrlItemType) || TYPE_PDF.equals(mtrlItemType);
    }

    /** 분석 대상 타입(01 이미지 / 04 PDF)인지 확인. 아니면 AI_400_006. */
    private void assertAnalyzable(TbmAiItemRow row) {
        if (!isVlmType(row.mtrlItemType())) {
            throw new ApiException(AiErrorCode.AI_400_006);
        }
    }

    /**
     * 교육자료 사업장 접근 게이트(쓰기 경로 — 위반 시 예외).
     *
     * <p>T2 {@code TbmAi02ServiceImpl.assertSiteAccess} + {@code RiskAi01ServiceImpl.assertSiteAccess}
     *    (tb_user_site_auth 매핑) + Tbm01 사업장 게이트를 대칭 복제한다. 회사 스코프(CMPNY_CD)만으로는
     *    동일 회사 내 타 사업장 자료의 AI 항목 열람/변조가 가능하므로 사업장 단위 격리를 강제한다.
     * <ul>
     *   <li>회사공통 자료(SITE_CD 비어있음): {@code canManageCommon}(master/safe)만 허용 → 아니면 TBM_403_002.</li>
     *   <li>사업장 자료: 회사 전체 권한(master/safe)은 통과, 그 외는 대상 사업장 권한 매핑
     *       (tb_user_site_auth USE_YN='Y') 보유 시에만 통과 → 없으면 TBM_403_003.</li>
     * </ul>
     */
    private void assertSiteAccess(String siteCd, TbmAiParam param) {
        boolean isCommon = (siteCd == null || siteCd.isEmpty());
        if (isCommon) {
            if (!AuthRoleUtils.canManageCommon(param.gvAuthCd())) {
                log.warn("TBM AI 회사공통 교육자료 접근 권한 없음 - authCd={}, userCd={}",
                    param.gvAuthCd(), param.gvUserCd());
                throw new ApiException(TbmErrorCode.TBM_403_002);
            }
            return;
        }
        if (AuthRoleUtils.isCompanyWide(param.gvAuthCd())) {
            return;
        }
        if (tbmAi01Mapper.countUserSiteAuth(param.gvCmpnyCd(), param.gvUserCd(), siteCd) == 0) {
            log.warn("TBM AI 타 사업장 교육자료 접근 시도 차단 - authCd={}, userCd={}, targetSite={}",
                param.gvAuthCd(), param.gvUserCd(), siteCd);
            throw new ApiException(TbmErrorCode.TBM_403_003);
        }
    }

    /**
     * 교육자료 사업장 접근 판정(읽기 경로 — analysis-status 용). {@link #assertSiteAccess}와 동일 규칙이나
     * 미권한 시 예외 대신 {@code false} 를 반환해 호출부가 존재 비노출(AI_404_002)로 수렴하게 한다.
     */
    private boolean hasSiteAccess(String siteCd, TbmAiParam param) {
        return hasSiteAccess(siteCd, param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd());
    }

    /**
     * 사업장 접근 판정(스칼라 인자형 — TbmAiParam 이 없는 내부 호출용, 예: {@link #enqueueOnSave}).
     * 판정 규칙은 {@link #assertSiteAccess} 와 동일하다.
     */
    private boolean hasSiteAccess(String siteCd, String cmpnyCd, String userCd, String authCd) {
        boolean isCommon = (siteCd == null || siteCd.isEmpty());
        if (isCommon) {
            return AuthRoleUtils.canManageCommon(authCd);
        }
        if (AuthRoleUtils.isCompanyWide(authCd)) {
            return true;
        }
        return tbmAi01Mapper.countUserSiteAuth(cmpnyCd, userCd, siteCd) > 0;
    }

    /** 타입 분기 VLM 이미지 로드(≤5 클램프). null/빈 → AI_404_002. */
    private List<ImagePart> buildVlmImagesOr404(String cmpnyCd, TbmAiItemRow row) {
        FileReadQuery query = new FileReadQuery(cmpnyCd, row.fileMgmtCd());
        List<ImagePart> out = new ArrayList<>();
        if (TYPE_IMAGE.equals(row.mtrlItemType())) {
            ImageBytesResult img = fileService.loadImageBytes(query);
            if (img == null || img.data() == null || img.data().length == 0) {
                throw new ApiException(AiErrorCode.AI_404_002);
            }
            out.add(new ImagePart(base64(img.data()), img.mediaType()));
        } else {
            int cap = Math.min(aiProperties.getTbm().getPdfMaxPages(), MAX_VLM_IMAGES);
            List<ImageBytesResult> pages =
                fileService.loadPdfPageImages(query, aiProperties.getTbm().getPdfPageStride(), cap);
            if (pages == null || pages.isEmpty()) {
                throw new ApiException(AiErrorCode.AI_404_002);
            }
            for (ImageBytesResult p : pages) {
                if (p == null || p.data() == null || p.data().length == 0) {
                    continue;
                }
                out.add(new ImagePart(base64(p.data()), p.mediaType()));
                if (out.size() >= MAX_VLM_IMAGES) {
                    break;
                }
            }
            if (out.isEmpty()) {
                throw new ApiException(AiErrorCode.AI_404_002);
            }
        }
        return out;
    }

    // ==================================================================
    // 내부 헬퍼 — 대화/응답 조립
    // ==================================================================

    /**
     * TbmAiChatTurn 목록 → HcxTurn 목록. ★이미지(들)는 "첫 user 턴"에만 부착한다
     * (HCX 요청당 이미지 ≤5 는 buildVlmImagesOr404 클램프로 자동 준수).
     */
    private List<HcxTurn> toHcxTurns(List<TbmAiChatTurn> turns, List<ImagePart> images) {
        List<HcxTurn> out = new ArrayList<>();
        boolean attached = false;
        for (TbmAiChatTurn t : turns) {
            if (t == null || t.role() == null) {
                continue;
            }
            List<ImagePart> turnImages = null;
            if (!attached && ROLE_USER.equals(t.role())) {
                turnImages = images;
                attached = true;
            }
            out.add(new HcxTurn(t.role(), t.text(), turnImages));
        }
        return out;
    }

    /** 대화 이력에서 가장 최근 assistant 발화 텍스트. 없으면 null. */
    private String latestAssistantText(List<TbmAiChatTurn> turns) {
        for (int i = turns.size() - 1; i >= 0; i--) {
            TbmAiChatTurn t = turns.get(i);
            if (t != null && ROLE_ASSISTANT.equals(t.role()) && StringUtils.hasText(t.text())) {
                return t.text();
            }
        }
        return null;
    }

    /** analysis-status 응답 조립(항목별 상태 + 대화턴 파싱). */
    private TbmAiAnalysisStatusResponse buildStatusResponse(String mtrlCd, String cmpnyCd) {
        List<TbmAiItemStatusRow> rows = tbmAi01Mapper.selectItemStatuses(mtrlCd, cmpnyCd);
        List<TbmAiAnalysisStatusResponse.ItemStatus> items = new ArrayList<>();
        if (rows != null) {
            for (TbmAiItemStatusRow r : rows) {
                if (r == null) {
                    continue;
                }
                items.add(TbmAiAnalysisStatusResponse.ItemStatus.of(r, parseChatTurns(r.aiChatJson())));
            }
        }
        return TbmAiAnalysisStatusResponse.from(mtrlCd, items, DISCLAIMER);
    }

    private List<TbmAiChatTurn> parseChatTurns(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<TbmAiChatTurn> v = objectMapper.readValue(json, new TypeReference<List<TbmAiChatTurn>>() {});
            return (v == null) ? new ArrayList<>() : v;
        } catch (Exception e) {
            log.warn("TBM AI 대화 이력 JSON 파싱 실패(빈 배열 대체) - 원인={}", e.getMessage());
            return new ArrayList<>();
        }
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

    // ==================================================================
    // 내부 헬퍼 — 감사·과금(best-effort, 2차 DS)
    // ==================================================================

    /** 감사 로그 기록. raw==null 이면 토큰/비용 0. 실패해도 응답은 성공(경고 로그). */
    private void recordCall(TbmAiParam param, String endpoint, LlmRawResponse raw, int queryLen) {
        try {
            long input = (raw == null) ? 0L : raw.inputTokens();
            long output = (raw == null) ? 0L : raw.outputTokens();
            long cacheRead = (raw == null) ? 0L : raw.cacheReadTokens();
            long cacheCreation = (raw == null) ? 0L : raw.cacheCreationTokens();
            String model = aiProperties.getLlm().getModel();
            BigDecimal cost = computeCost(input, output);

            AiCallLog logRow = new AiCallLog(
                UUID.randomUUID().toString(),
                param.gvUserCd(),
                param.gvCmpnyCd(),
                endpoint,
                model,
                queryLen,               // ★원문 미저장(길이만)
                input, output, cacheRead, cacheCreation,
                cost,
                List.of(),              // RAG 미사용 → 사용 청크 없음
                false);
            aiCallRepository.save(logRow);
        } catch (Exception e) {
            log.warn("tb_ai_call 감사 로깅 실패(응답은 정상 반환) - 원인={}", e.getMessage());
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

    /** 트림 + 길이 클램프(컬럼 절단 방어). null 은 그대로. */
    private String clamp(String v, int max) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
