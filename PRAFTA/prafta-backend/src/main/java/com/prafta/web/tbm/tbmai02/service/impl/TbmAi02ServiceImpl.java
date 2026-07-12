package com.prafta.web.tbm.tbmai02.service.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import com.prafta.app.ai.ai01.application.model.AiCallLog;
import com.prafta.app.ai.ai01.client.LlmAnswerClient;
import com.prafta.app.ai.ai01.client.LlmRawResponse;
import com.prafta.app.ai.ai01.repository.AiCallRepository;
import com.prafta.common.config.AiProperties;
import com.prafta.common.error.ai.AiErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbmai02.application.model.TbmSessionGenSource;
import com.prafta.web.tbm.tbmai02.application.model.TbmUnconfirmedAiItem;
import com.prafta.web.tbm.tbmai02.application.param.TbmAi02Param;
import com.prafta.web.tbm.tbmai02.dto.response.TbmAiUnconfirmedResponse;
import com.prafta.web.tbm.tbmai02.dto.response.TbmGenerateResponse;
import com.prafta.web.tbm.tbmai02.mapper.TbmAi02Mapper;
import com.prafta.web.tbm.tbmai02.service.TbmAi02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TBM AI 교육안 생성 서비스 구현(TBM_AI v4 RC).
 *
 * <p>★설계 요지:
 *   <ol>
 *     <li>생성 입력 = 세션(sessionCd)에 묶인 교육자료 항목 중 CONFIRMED+확정 서술(AI_CONFIRM_DESC) 비공백 전부
 *         + 관리자 교육내용 텍스트. RAG 코퍼스 미사용(근거는 확정 자료·관리자 입력).
 *         미확정 AI 분석 항목은 차단하지 않고 건너뛴다(2026-07-11 기획 변경) —
 *         단, 확정 자료 0건이면 TBM_409_060("분석할 자료가 없습니다")으로 거부.</li>
 *     <li>출력 = 라인 프로토콜 4섹션(작업개요/핵심 위험요인/안전수칙/오늘의 확인사항). JSON 미채택
 *         (HCX-005 중첩 JSON 취약). 1차 파싱 실패 시 동일 프롬프트 1회 재시도, 재시도도 실패면 AI_502_004.
 *         최종 산출물은 세션 CONTENT_BODY(RICH_HTML) 대상 리치HTML로 렌더한다.</li>
 *     <li>DB 미기록: generate 는 초안(HTML)만 반환한다. 영속은 FE가 세션 CONTENT_BODY 에디터에 채운 뒤
 *         기존 세션 저장 경로로 처리한다(tbm01/tbm02 미터치).</li>
 *     <li>회사 스코프/IDOR: sessionCd 회사 소유 검증(selectSessionGenSource CMPNY_CD), userCd/cmpnyCd/authCd 는
 *         JWT 클레임에서만. 세션 SITE_CD(NOT NULL) 기준 사업장 격리.</li>
 *     <li>과금·감사 = tb_ai_call 원장 재사용(endpoint="tbmai02/generate"), best-effort.</li>
 *   </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TbmAi02ServiceImpl implements TbmAi02Service {

    private final TbmAi02Mapper tbmAi02Mapper;
    private final LlmAnswerClient llmAnswerClient;
    private final AiCallRepository aiCallRepository;
    private final AiProperties aiProperties;

    /** tb_ai_call 감사 엔드포인트 식별자(모듈별 복제 패턴). */
    private static final String ENDPOINT_GENERATE = "tbmai02/generate";

    /** 라인 프로토콜 파싱 실패 시 동일 프롬프트 재시도 횟수(매직넘버 금지). */
    private static final int RETRY_COUNT = 1;

    /** 컨텍스트 총 길이 상한(프롬프트 폭주 방어). */
    private static final int MAX_CONTEXT_LEN = 8000;

    /** 확정 안전정보 항목 1건 길이 상한(개별 서술 방어 절단). */
    private static final int MAX_ITEM_DESC_LEN = 2000;

    /** GEN_AT 응답 표기 포맷(서버 시각). */
    private static final DateTimeFormatter GEN_AT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 교육안 생성 허용 세션 상태(SYS046). COMPLETED/CANCELLED 는 편집 불가 → 생성 차단. */
    private static final Set<String> GENERATABLE_STATUS = Set.of("DRAFT", "OPENED", "IN_PROGRESS");

    /** 확정 자료 0건 시 생성 거부 안내(2026-07-11 기획 변경 — 미확정은 건너뛰되 확정 전무면 생성 불가). */
    private static final String MSG_NO_CONFIRMED_ITEM =
          "분석할 자료가 없습니다.\n\n"
        + "AI 분석이 확정된 자료가 없어 교육안을 생성할 수 없습니다.\n"
        + "[AI 분석 관리] 탭에서 자료 내용을 확정한 뒤 다시 시도해 주세요.";

    /** 자체 VLM 분석 대상 타입(이미지 01 / PDF 04) — NONE 라벨을 "분석대기"로 표기. */
    private static final Set<String> VLM_ANALYZE_TYPES = Set.of("01", "04");

    /**
     * AI 상태 → 사용자 노출 라벨 매핑(FE TbmItemAiPanel 파생 규칙과 일치).
     * NONE/NULL 은 타입에 따라 분기하므로 여기서 제외하고 {@link #resolveStatusLabel(String, String)} 에서 처리한다.
     */
    private static final Map<String, String> AI_STATUS_LABEL = Map.of(
        "ANALYZING", "분석중",
        "DRAFT", "분석완료-미확정",
        "FAILED", "분석실패");

    /** NONE/NULL 미확정 라벨: VLM 대상(01/04)은 "분석대기", 그 외(동영상 02/유튜브 03)는 "관리자 확정 대기". */
    private static final String LABEL_WAIT_ANALYZE = "분석대기";
    private static final String LABEL_WAIT_MANUAL = "관리자 확정 대기";

    /**
     * 위 매핑에 없는 상태의 기본 미확정 라벨. 실질적으로 AI_STATUS='CONFIRMED' 인데
     * AI_CONFIRM_DESC 가 공백인 이상 데이터에서만 나온다(교육안 입력으로 쓸 확정 서술이 없어 미확정 취급).
     * 관리자가 "확정했는데 왜 막히지"로 혼란하지 않도록 원인을 드러내는 문구를 쓴다.
     */
    private static final String LABEL_UNCONFIRMED_FALLBACK = "확정 서술 없음";

    // 라인 프로토콜 4섹션 캐논 키(고정 순서).
    private static final String SEC_OVERVIEW = "작업개요";
    private static final String SEC_HAZARD = "핵심 위험요인";
    private static final String SEC_SAFETY = "안전수칙";
    private static final String SEC_CHECK = "오늘의 확인사항";
    private static final String[] SECTION_ORDER = { SEC_OVERVIEW, SEC_HAZARD, SEC_SAFETY, SEC_CHECK };

    /**
     * 교육안 생성 시스템 프롬프트(few-shot 포함).
     *
     * <p>★few-shot 은 제조업 시나리오 자체 창작(프레스 금형 교체 / 지게차 자재 운반) —
     *    KOSHA TBM 가이드의 <b>4섹션 양식 구조만</b> 차용하고 원문 문장은 복붙하지 않는다(저작권/라이선스).
     * <p>목표 글자수(min/max)는 상수 하드코딩 금지 — {@link #buildSystemPrompt()} 에서 설정값을 주입한다.
     */
    private static final String SYSTEM_PROMPT_TEMPLATE =
          "당신은 제조업 현장의 산업안전 교육(TBM, Tool Box Meeting) 자료 작성 보조자다.\n"
        + "아래 [교육 주제]·[관리자 교육내용]·[확정 안전정보]만을 근거로 오늘의 TBM 교육안을 작성한다.\n"
        + "1. 제공된 정보에 없는 사실·수치·설비명을 지어내지 않는다. 확정 안전정보가 빈약하면 일반적인\n"
        + "   제조업 안전 원칙 범위에서 보수적으로 서술한다.\n"
        + "2. 반드시 아래 4개 섹션을 이 순서와 헤더 문구 그대로 출력한다(섹션 누락 금지):\n"
        + "   ### 작업개요\n"
        + "   ### 핵심 위험요인\n"
        + "   ### 안전수칙\n"
        + "   ### 오늘의 확인사항\n"
        + "3. '작업개요'는 1~3줄 서술문으로, 나머지 3개 섹션은 각 항목을 '- ' 불릿으로 나열한다.\n"
        + "4. 현장 작업자가 바로 이해할 수 있게 간결한 한국어 실무 문장으로 쓴다.\n"
        + "5. 전체 분량은 약 %d~%d자를 목표로 한다.\n"
        + "6. 머리말·맺음말·JSON·표는 출력하지 않는다. 위 4개 섹션 헤더와 본문만 출력한다.\n"
        + "\n"
        + "[출력 예시 1 — 프레스 금형 교체 작업]\n"
        + "### 작업개요\n"
        + "오늘은 200톤 유압 프레스의 금형을 교체한다. 금형 탈착과 시운전 과정에서 협착·낙하 위험이 크므로\n"
        + "전원 차단과 2인 1조 작업을 기본으로 한다.\n"
        + "### 핵심 위험요인\n"
        + "- 프레스 슬라이드 하강에 의한 손·팔 협착\n"
        + "- 중량 금형 인양·이동 중 낙하 및 발 협착\n"
        + "- 잔압·오작동에 의한 예기치 못한 슬라이드 작동\n"
        + "### 안전수칙\n"
        + "- 금형 교체 전 메인 전원 차단 및 잠금표지(LOTO) 부착\n"
        + "- 안전블록 설치 후 슬라이드 하부 작업 진행\n"
        + "- 중량물 인양은 정격 호이스트·슬링 사용, 하부 진입 금지\n"
        + "### 오늘의 확인사항\n"
        + "- 전원 차단·잠금표지 부착 여부 상호 확인\n"
        + "- 안전블록·양수조작 버튼 정상 작동 점검\n"
        + "- 보호장갑·안전화 착용 상태 점검\n"
        + "\n"
        + "[출력 예시 2 — 지게차 자재 운반 작업]\n"
        + "### 작업개요\n"
        + "오늘은 지게차로 원자재 파렛트를 창고에서 생산라인으로 운반한다. 보행자 통행이 잦은 구간을\n"
        + "지나므로 충돌·전도 예방에 집중한다.\n"
        + "### 핵심 위험요인\n"
        + "- 지게차와 보행 작업자의 충돌\n"
        + "- 과적·편하중에 의한 지게차 전도\n"
        + "- 적재물 낙하에 의한 협착\n"
        + "### 안전수칙\n"
        + "- 지정 운행 경로·제한속도 준수, 교차로 일단정지 후 경적\n"
        + "- 적재 하중·높이 기준 준수, 포크 하강 후 이동\n"
        + "- 보행자-지게차 동선 분리 및 유도자 배치\n"
        + "### 오늘의 확인사항\n"
        + "- 지게차 후진 경보·경광등 작동 점검\n"
        + "- 적재물 결속·편하중 여부 확인\n"
        + "- 운행 경로상 보행자 통제 여부 확인\n";

    // ==================================================================
    // 교육안 생성(세션 단위)
    // ==================================================================

    /**
     * ★트랜잭션 경계 주의: 본 메서드는 선행 SELECT + 동기 LLM 호출(재시도 최대 1회)만 포함하고
     *   DB 쓰기가 전혀 없으므로(초안 반환) @Transactional 로 감싸지 않는다.
     *   (감사 로깅은 best-effort 별도 저장.)
     */
    @Override
    public TbmGenerateResponse generate(TbmAi02Param param) {
        log.info("TBM AI 교육안 생성 진입 - cmpnyCd={}, userCd={}, sessionCd={}",
            param.gvCmpnyCd(), param.gvUserCd(), param.sessionCd());

        // 파라미터/권한 사전거부.
        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // 999999(권한 미부여)는 생성 차단(세션 편집 권한과 대칭).
        if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
            log.warn("TBM AI 교육안 생성 접근 차단 - authCd={}", param.gvAuthCd());
            throw new ApiException(TbmErrorCode.TBM_403_001);
        }

        // 회사 소유 검증(IDOR): 없으면 TBM_404_010(존재 미노출).
        TbmSessionGenSource source = tbmAi02Mapper.selectSessionGenSource(param.sessionCd(), param.gvCmpnyCd());
        if (source == null) {
            log.warn("TBM AI 교육안 생성 - 타 회사/미존재 sessionCd 차단 - sessionCd={}", param.sessionCd());
            throw new ApiException(TbmErrorCode.TBM_404_010);
        }

        // 세션 상태 게이트: COMPLETED/CANCELLED 등 편집 불가 상태에서는 생성 차단.
        if (!GENERATABLE_STATUS.contains(source.statusCd())) {
            log.warn("TBM AI 교육안 생성 - 편집 불가 세션 상태 - sessionCd={}, statusCd={}",
                param.sessionCd(), source.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_010);
        }

        // 사업장 격리: 세션 SITE_CD(NOT NULL) 기준 접근 강제(회사 스코프만으로는 동일 회사 타 사업장 자료 노출 방지).
        assertSiteAccess(source, param);

        // ★미확정 AI 분석 항목은 차단하지 않고 건너뛴다(2026-07-11 기획 변경 — 확정 항목만 입력으로 사용).
        //   단, 확정된 자료가 0건이면 분석할 근거가 없으므로 생성을 거부한다(관리자 교육내용만으로는 생성하지 않음).

        // 입력 조립: 세션 묶인 확정 안전정보 + 관리자 교육내용.
        String adminText = safeTrim(param.adminContentText());
        List<String> confirmedDescs = tbmAi02Mapper.selectSessionConfirmedItemDescs(param.sessionCd(), param.gvCmpnyCd());

        boolean hasAdmin = StringUtils.hasText(adminText);
        int includedItemCount = countNonBlank(confirmedDescs);
        // 확정 자료 전무 → 분석할 자료 없음 안내(LLM 미호출).
        if (includedItemCount == 0) {
            log.warn("TBM AI 교육안 생성 거부 - 확정 자료 0건 - sessionCd={}", param.sessionCd());
            throw new ApiException(TbmErrorCode.TBM_409_060, MSG_NO_CONFIRMED_ITEM);
        }
        // 관리자 교육내용 미입력 → 확정 서술만으로 추정 생성(FE 품질저하 안내).
        boolean qualityDegraded = !hasAdmin;

        // 게이트 OFF → 503.
        if (!llmAnswerClient.isEnabled()) {
            throw new ApiException(AiErrorCode.AI_503_001);
        }

        String context = buildGenerateContext(source.title(), adminText, confirmedDescs);
        String system = buildSystemPrompt();

        // 1차 호출 → 파싱. 실패 시 1회 재시도(실패한 1차 호출도 감사 기록).
        LlmRawResponse raw = llmAnswerClient.answer(system, context);
        Map<String, String> sections = raw.refusal() ? null : parseTbmPlan(raw.combinedText());

        for (int attempt = 0; attempt < RETRY_COUNT && sections == null; attempt++) {
            recordCall(param, raw, context.length(), true);
            log.warn("TBM 교육안 라인프로토콜 파싱 실패 - 동일 프롬프트로 1회 재시도");
            raw = llmAnswerClient.answer(system, context);
            sections = raw.refusal() ? null : parseTbmPlan(raw.combinedText());
        }

        // 재시도도 실패 → AI_502_004(파싱 실패). 마지막 실패 호출은 감사 기록.
        if (sections == null) {
            recordCall(param, raw, context.length(), true);
            log.error("TBM 교육안 라인프로토콜 파싱 최종 실패 - sessionCd={}", param.sessionCd());
            throw new ApiException(AiErrorCode.AI_502_004);
        }

        // 길이 클램프(항목 단위 누적, HTML 태그 중간 절단 방지) 후 리치HTML 렌더.
        Map<String, String> clamped = clampSectionsToMax(sections);
        String genContent = renderTbmPlanHtml(clamped);

        // 감사·과금(성공 호출, best-effort). ※ DB 미기록 — 세션 CONTENT_BODY 는 FE 저장 경로로 영속.
        recordCall(param, raw, context.length(), false);

        String genAt = LocalDateTime.now().format(GEN_AT_FMT);
        log.info("TBM AI 교육안 생성 완료 - sessionCd={}, length={}, includedItemCount={}, qualityDegraded={}",
            param.sessionCd(), genContent.length(), includedItemCount, qualityDegraded);
        return TbmGenerateResponse.builder()
            .genContent(genContent)
            .genAt(genAt)
            .qualityDegraded(qualityDegraded)
            .includedItemCount(includedItemCount)
            .build();
    }

    // ==================================================================
    // 미확정 AI 분석 항목 조회(FE 사전 차단용, 조회 전용)
    // ==================================================================

    /**
     * generate 와 동일한 사전 게이트(파라미터/권한/회사소유/사업장)만 적용하고 목록을 반환한다.
     * 세션 상태 게이트(GENERATABLE_STATUS)·LLM 게이트는 검사하지 않는다(게이트 OFF 에서도 목록은 노출).
     * LLM 미호출·감사 미기록.
     */
    @Override
    public TbmAiUnconfirmedResponse unconfirmedItems(TbmAi02Param param) {
        log.info("TBM AI 미확정 항목 조회 진입 - cmpnyCd={}, userCd={}, sessionCd={}",
            param.gvCmpnyCd(), param.gvUserCd(), param.sessionCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
            log.warn("TBM AI 미확정 항목 조회 접근 차단 - authCd={}", param.gvAuthCd());
            throw new ApiException(TbmErrorCode.TBM_403_001);
        }
        TbmSessionGenSource source = tbmAi02Mapper.selectSessionGenSource(param.sessionCd(), param.gvCmpnyCd());
        if (source == null) {
            log.warn("TBM AI 미확정 항목 조회 - 타 회사/미존재 sessionCd 차단 - sessionCd={}", param.sessionCd());
            throw new ApiException(TbmErrorCode.TBM_404_010);
        }
        assertSiteAccess(source, param);

        List<TbmUnconfirmedAiItem> unconfirmed =
            tbmAi02Mapper.selectSessionUnconfirmedAiItems(param.sessionCd(), param.gvCmpnyCd());
        if (unconfirmed == null) {
            unconfirmed = List.of();
        }

        List<TbmAiUnconfirmedResponse.Item> items = new ArrayList<>(unconfirmed.size());
        for (TbmUnconfirmedAiItem u : unconfirmed) {
            items.add(TbmAiUnconfirmedResponse.Item.builder()
                .mtrlTitle(u.mtrlTitle())
                .mtrlItemCd(u.mtrlItemCd())
                .mtrlItemType(u.mtrlItemType())
                .mtrlDesc(u.mtrlDesc())
                .aiStatus(u.aiStatus())
                .statusLabel(resolveStatusLabel(u.aiStatus(), u.mtrlItemType()))
                .build());
        }

        log.info("TBM AI 미확정 항목 조회 완료 - sessionCd={}, count={}", param.sessionCd(), items.size());
        return TbmAiUnconfirmedResponse.builder()
            .count(items.size())
            .items(items)
            .build();
    }

    // ==================================================================
    // 내부 헬퍼 — 미확정 항목 상태 라벨(조회 EP 노출용)
    // ==================================================================

    /**
     * AI 상태 → 사용자 노출 라벨(FE TbmItemAiPanel 파생 규칙과 동일).
     * NONE/NULL 은 타입 분기(01·04=분석대기, 그 외=관리자 확정 대기), 그 외는 매핑 사용, 미매핑은 기본 미확정 라벨.
     */
    private String resolveStatusLabel(String aiStatus, String mtrlItemType) {
        String status = (aiStatus == null) ? "" : aiStatus.trim();
        if (status.isEmpty() || "NONE".equals(status)) {
            return VLM_ANALYZE_TYPES.contains(mtrlItemType) ? LABEL_WAIT_ANALYZE : LABEL_WAIT_MANUAL;
        }
        return AI_STATUS_LABEL.getOrDefault(status, LABEL_UNCONFIRMED_FALLBACK);
    }

    // ==================================================================
    // 내부 헬퍼 — 사업장 격리(세션 SITE_CD NOT NULL 전제, RiskAi01 assertSiteAccess 패턴)
    // ==================================================================

    /**
     * 세션 사업장 접근 게이트(위반 시 예외).
     *
     * <p>세션 SITE_CD 는 NOT NULL 이므로 회사공통 분기 없이 사업장 분기만 유효하다.
     * 회사 전체 권한(master/safe)은 통과, 그 외는 대상 사업장 권한 매핑(tb_user_site_auth USE_YN='Y')
     * 보유 시에만 통과 → 없으면 TBM_403_003.
     */
    private void assertSiteAccess(TbmSessionGenSource source, TbmAi02Param param) {
        if (AuthRoleUtils.isCompanyWide(param.gvAuthCd())) {
            return;
        }
        if (tbmAi02Mapper.countUserSiteAuth(param.gvCmpnyCd(), param.gvUserCd(), source.siteCd()) == 0) {
            log.warn("TBM AI 타 사업장 세션 접근 시도 차단 - authCd={}, userCd={}, targetSite={}, sessionCd={}",
                param.gvAuthCd(), param.gvUserCd(), source.siteCd(), source.sessionCd());
            throw new ApiException(TbmErrorCode.TBM_403_003);
        }
    }

    // ==================================================================
    // 내부 헬퍼 — 프롬프트/컨텍스트 조립
    // ==================================================================

    /** 목표 글자수(설정값)를 주입해 시스템 프롬프트 조립(하드코딩 금지). */
    private String buildSystemPrompt() {
        int min = aiProperties.getTbm().getTbmContentMinChars();
        int max = aiProperties.getTbm().getTbmContentMaxChars();
        return String.format(SYSTEM_PROMPT_TEMPLATE, min, max);
    }

    /**
     * 생성 컨텍스트 조립: [교육 주제] → [관리자 교육내용](있으면) → [확정 안전정보] 번호목록 → [분량 지침].
     * 총 길이 상한 클램프(프롬프트 폭주 방어).
     */
    private String buildGenerateContext(String title, String adminText, List<String> confirmedDescs) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(title)) {
            sb.append("[교육 주제] ").append(title.trim()).append('\n');
        }
        if (StringUtils.hasText(adminText)) {
            sb.append("[관리자 교육내용]\n").append(adminText.trim()).append('\n');
        }
        if (confirmedDescs != null && !confirmedDescs.isEmpty()) {
            sb.append("[확정 안전정보]\n");
            int n = 1;
            for (String desc : confirmedDescs) {
                if (!StringUtils.hasText(desc)) {
                    continue;
                }
                String d = desc.trim();
                if (d.length() > MAX_ITEM_DESC_LEN) {
                    d = d.substring(0, MAX_ITEM_DESC_LEN);
                }
                sb.append(n++).append(". ").append(d).append('\n');
            }
        }
        int min = aiProperties.getTbm().getTbmContentMinChars();
        int max = aiProperties.getTbm().getTbmContentMaxChars();
        sb.append("[분량 지침] 약 ").append(min).append('~').append(max)
          .append("자, 4개 섹션(작업개요/핵심 위험요인/안전수칙/오늘의 확인사항) 고정");

        String ctx = sb.toString();
        return ctx.length() > MAX_CONTEXT_LEN ? ctx.substring(0, MAX_CONTEXT_LEN) : ctx;
    }

    // ==================================================================
    // 내부 헬퍼 — 라인 프로토콜 파싱/렌더
    // ==================================================================

    /**
     * 라인 프로토콜 파싱: 줄 순회로 4섹션(작업개요/핵심 위험요인/안전수칙/오늘의 확인사항)을 분리한다.
     *
     * <ul>
     *   <li>섹션 헤더 줄('#'/'[' 접두 또는 캐논 키 정확 일치)이면 현재 섹션 전환.</li>
     *   <li>그 외 비공백 줄은 불릿 제거 후 현재 섹션 버퍼에 append(섹션 미시작 줄은 warn 후 skip).</li>
     * </ul>
     * 4섹션이 모두 비공백이면 섹션 맵 반환, 하나라도 공백/누락이면 null(→ 재시도/실패).
     */
    private Map<String, String> parseTbmPlan(String combinedText) {
        if (combinedText == null || combinedText.isBlank()) {
            log.error("TBM 교육안 LLM 응답이 비어 있음");
            return null;
        }

        Map<String, StringBuilder> buffers = new LinkedHashMap<>();
        for (String key : SECTION_ORDER) {
            buffers.put(key, new StringBuilder());
        }

        String current = null;
        for (String rawLine : combinedText.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            String header = matchSectionHeader(line);
            if (header != null) {
                current = header;
                continue;
            }
            if (current == null) {
                log.warn("섹션 미시작 상태의 본문 줄 skip - line={}", line);
                continue;
            }
            String body = stripBullet(line);
            if (body.isEmpty()) {
                continue;
            }
            StringBuilder buf = buffers.get(current);
            if (buf.length() > 0) {
                buf.append('\n');
            }
            buf.append(body);
        }

        Map<String, String> sections = new LinkedHashMap<>();
        for (String key : SECTION_ORDER) {
            String v = buffers.get(key).toString().trim();
            if (v.isEmpty()) {
                log.error("TBM 교육안 섹션 누락/공백 - section={}", key);
                return null;
            }
            sections.put(key, v);
        }
        return sections;
    }

    /**
     * 헤더 줄 판정: '#' 접두이거나 '[...]' 형식인 줄에서 마크다운/괄호를 제거한 텍스트가
     * 캐논 4키 중 하나와 일치(contains)하면 해당 캐논 키 반환, 아니면 null.
     */
    private String matchSectionHeader(String line) {
        boolean looksHeader = line.charAt(0) == '#' || (line.startsWith("[") && line.endsWith("]"));
        String norm = line;
        // 선행 '#', 공백 제거.
        int i = 0;
        while (i < norm.length() && (norm.charAt(i) == '#' || norm.charAt(i) == ' ')) {
            i++;
        }
        norm = norm.substring(i).trim();
        // 감싼 대괄호 제거.
        if (norm.startsWith("[") && norm.endsWith("]") && norm.length() >= 2) {
            norm = norm.substring(1, norm.length() - 1).trim();
        }
        for (String key : SECTION_ORDER) {
            if (norm.equals(key)) {
                return key;
            }
            if (looksHeader && norm.contains(key)) {
                return key;
            }
        }
        return null;
    }

    /**
     * 리치HTML 렌더: 파싱된 4섹션을 세션 CONTENT_BODY(CONTENT_FORMAT_CD=RICH_HTML, QuillEditor html)
     * 형식으로 변환한다.
     *
     * <ul>
     *   <li>작업개요(서술문): {@code <h4>작업개요</h4><p>…</p>} (줄바꿈은 {@code <br>}).</li>
     *   <li>나머지 3섹션(불릿 다건): {@code <h4>제목</h4><ul><li>항목</li>…</ul>}.</li>
     *   <li>각 항목/서술은 {@link HtmlUtils#htmlEscape(String)} 로 이스케이프 후 삽입(LLM 출력 내
     *       {@code <}/{@code >} 의 태그 오인·XSS 방지). Quill 친화 태그({@code h4/p/ul/li/br})만 사용.</li>
     * </ul>
     */
    private String renderTbmPlanHtml(Map<String, String> sections) {
        StringBuilder sb = new StringBuilder();
        for (String key : SECTION_ORDER) {
            String body = sections.get(key);
            if (!StringUtils.hasText(body)) {
                continue;
            }
            sb.append("<h4>").append(HtmlUtils.htmlEscape(key)).append("</h4>");
            String[] lines = body.split("\\n");
            if (SEC_OVERVIEW.equals(key)) {
                // 서술문: 문단 하나(<p>), 줄바꿈은 <br>.
                sb.append("<p>");
                boolean first = true;
                for (String ln : lines) {
                    String t = ln.trim();
                    if (t.isEmpty()) {
                        continue;
                    }
                    if (!first) {
                        sb.append("<br>");
                    }
                    sb.append(HtmlUtils.htmlEscape(t));
                    first = false;
                }
                sb.append("</p>");
            } else {
                // 불릿 목록.
                sb.append("<ul>");
                for (String ln : lines) {
                    String t = ln.trim();
                    if (t.isEmpty()) {
                        continue;
                    }
                    sb.append("<li>").append(HtmlUtils.htmlEscape(t)).append("</li>");
                }
                sb.append("</ul>");
            }
        }
        return sb.toString();
    }

    /**
     * 길이 클램프: 섹션·항목(줄) 단위로 plain 글자수를 누적하며 최대 글자수 도달 시 이후 항목을 절단한다
     * (HTML 태그 중간 절단 방지 — 렌더 전 plain 상태에서 트림). 최소 글자수는 프롬프트 목표로만 유도.
     *
     * <p>한 항목도 담기 전에 예산을 초과하면 최소 1개 항목은 remaining 만큼 절단해 담는다(빈 출력 방지).
     */
    private Map<String, String> clampSectionsToMax(Map<String, String> sections) {
        int max = aiProperties.getTbm().getTbmContentMaxChars();
        Map<String, String> out = new LinkedHashMap<>();
        int used = 0;
        boolean anyKept = false;
        boolean stop = false;

        for (String key : SECTION_ORDER) {
            if (stop) {
                break;
            }
            String body = sections.get(key);
            if (!StringUtils.hasText(body)) {
                continue;
            }
            // 헤더 키 길이도 예산에 반영(근사).
            used += key.length();
            StringBuilder secBuf = new StringBuilder();
            for (String ln : body.split("\\n")) {
                String t = ln.trim();
                if (t.isEmpty()) {
                    continue;
                }
                int cost = t.length();
                if (used + cost > max) {
                    if (!anyKept) {
                        // 최소 1개 항목은 담는다(잔여 예산만큼 절단).
                        int remaining = Math.max(0, max - used);
                        if (remaining > 0) {
                            String cut = t.substring(0, Math.min(remaining, t.length()));
                            secBuf.append(cut);
                            anyKept = true;
                        }
                    }
                    stop = true;
                    break;
                }
                if (secBuf.length() > 0) {
                    secBuf.append('\n');
                }
                secBuf.append(t);
                used += cost;
                anyKept = true;
            }
            if (secBuf.length() > 0) {
                out.put(key, secBuf.toString());
            }
        }
        // 방어: 전량 트림되어 비면 원본 첫 섹션 그대로 반환(빈 초안 방지).
        return out.isEmpty() ? sections : out;
    }

    /** 선행 불릿 기호('-', '•', '*', '▶') 제거 + trim. */
    private String stripBullet(String line) {
        String out = line;
        while (!out.isEmpty()) {
            char c = out.charAt(0);
            if (c == '-' || c == '•' || c == '*' || c == '▶') {
                out = out.substring(1).trim();
            } else {
                break;
            }
        }
        return out;
    }

    // ==================================================================
    // 내부 헬퍼 — 감사·과금(best-effort, riskai01 복제)
    // ==================================================================

    /** 감사 로그 기록. raw==null 이면 토큰/비용 0. 실패해도 응답은 성공(경고 로그). */
    private void recordCall(TbmAi02Param param, LlmRawResponse raw, int queryLen, boolean abstained) {
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
                ENDPOINT_GENERATE,
                model,
                queryLen,               // ★원문 미저장(길이만)
                input, output, cacheRead, cacheCreation,
                cost,
                List.of(),              // RAG 미사용 — 청크 없음
                abstained);
            aiCallRepository.save(logRow);
        } catch (Exception e) {
            log.warn("tb_ai_call 감사 로깅 실패(응답은 정상 반환) - 원인={}", e.getMessage());
        }
    }

    /**
     * HCX 과금 계산: 설정 단가(1k 토큰당, 기본 0) 기반. 캐시 단가 없음.
     * (RiskAi01ServiceImpl.computeCost / AnswerServiceImpl.computeCost 와 동일 정책 — 산식 일원화.)
     */
    private BigDecimal computeCost(long input, long output) {
        double inRate = aiProperties.getLlm().getCostPer1kInput();
        double outRate = aiProperties.getLlm().getCostPer1kOutput();
        double cost = (input / 1_000d) * inRate
                    + (output / 1_000d) * outRate;
        return new BigDecimal(cost, MathContext.DECIMAL64).setScale(5, RoundingMode.HALF_UP);
    }

    private String safeTrim(String v) {
        return (v == null) ? "" : v.trim();
    }

    /** 비공백 확정 서술 항목 수(제외항목 안내용 includedItemCount). */
    private int countNonBlank(List<String> descs) {
        if (descs == null) {
            return 0;
        }
        int n = 0;
        for (String d : descs) {
            if (StringUtils.hasText(d)) {
                n++;
            }
        }
        return n;
    }
}
