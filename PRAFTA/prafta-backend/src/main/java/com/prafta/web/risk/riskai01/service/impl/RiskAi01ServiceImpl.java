package com.prafta.web.risk.riskai01.service.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.app.ai.ai01.application.model.AiCallLog;
import com.prafta.app.ai.ai01.application.param.RagSearchParam;
import com.prafta.app.ai.ai01.client.HcxTurn;
import com.prafta.app.ai.ai01.client.ImagePart;
import com.prafta.app.ai.ai01.client.LlmAnswerClient;
import com.prafta.app.ai.ai01.client.LlmRawResponse;
import com.prafta.app.ai.ai01.dto.response.RagHit;
import com.prafta.app.ai.ai01.dto.response.RagSearchResponse;
import com.prafta.app.ai.ai01.repository.AiCallRepository;
import com.prafta.app.ai.ai01.service.Ai01Service;
import com.prafta.common.cmm.ai.EvidenceTierLabels;
import com.prafta.common.cmm.aiquota.service.AiQuotaService;
import com.prafta.common.cmm.file.application.model.ImageBytesResult;
import com.prafta.common.cmm.file.application.query.FileReadQuery;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.config.AiProperties;
import com.prafta.common.error.ai.AiErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.risk.riskai01.application.model.DeriveLlmResult;
import com.prafta.web.risk.riskai01.application.model.RiskAiDerivationRow;
import com.prafta.web.risk.riskai01.application.model.RiskAssessmentAiSource;
import com.prafta.web.risk.riskai01.application.param.RiskAiParam;
import com.prafta.web.risk.riskai01.dto.request.RiskAiRequest;
import com.prafta.web.risk.riskai01.dto.response.ChatTurn;
import com.prafta.web.risk.riskai01.dto.response.CitationItem;
import com.prafta.web.risk.riskai01.dto.response.DerivedHazardGroup;
import com.prafta.web.risk.riskai01.dto.response.DerivedItem;
import com.prafta.web.risk.riskai01.dto.response.LawRefItem;
import com.prafta.web.risk.riskai01.dto.response.RiskAiDerivationResponse;
import com.prafta.web.risk.riskai01.dto.response.VerbatimRefItem;
import com.prafta.web.risk.riskai01.mapper.RiskAi01Mapper;
import com.prafta.web.risk.riskai01.service.RiskAi01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 위험성평가 AI 유해요인·개선안 도출 서비스 구현(PRAFTA-WEB_003 v3 절차 기반).
 *
 * <p>★확정 결정(v3):
 *   <ol>
 *     <li>이미지 분석 = 확인 질의형 확정 루프(chatImage). VLM 이 "…로 보입니다. 맞습니까?" 로 재질의하고,
 *         Yes(confirmImage) 확정까지 반복. kickoff(자동 첫 질의)는 숨김 user 턴(hidden=true)으로 영속.</li>
 *     <li>★이미지 매 요청 재부착: IMG_CHAT_JSON 엔 텍스트만 저장하고, chat 호출 때마다 디스크에서
 *         이미지를 다시 로드해 재구성 turns 의 "첫 user 턴"에 재부착한다(HCX stateless).
 *         관리자 추가 이미지(adminImages)는 요청단발 — 이번 user 턴에만 부착하고 영속하지 않는다.</li>
 *     <li>도출 결과 = 유해요인별 개선안 그룹 [{text,markers[],measures:[{text,markers[]}]}] 로
 *         HAZARD_JSON 에 저장(MEASURE_JSON 은 NULL 클리어). RAG·track분리(fail-closed)·citation 서버대조 유지.</li>
 *     <li>★캡(횟수 제한) 전면 제거. 사업장 게이트(countUserSiteAuth assertSiteAccess) + 회사 스코프 유지.</li>
 *     <li>★하드가드 #4 개정(v3.9): verbatim(3유형·변경금지) 원문은 생성 호출(그라운딩/자유생성)에는
 *         절대 미투입. 단 "선택 전용 호출"(selectVerbatimRefs)에는 입력 가능 — 출력이 번호로 제한되고
 *         서버가 숫자 외 출력을 폐기하며 화면은 DB 원문 그대로 렌더하므로 원문 변형 배포가 구조적으로 불가.</li>
 *   </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAi01ServiceImpl implements RiskAi01Service {

    private final RiskAi01Mapper riskAi01Mapper;
    private final Ai01Service ai01Service;
    private final LlmAnswerClient llmAnswerClient;
    private final FileService fileService;
    private final AiCallRepository aiCallRepository;
    private final AiQuotaService aiQuotaService;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    /** derive 질의 최대 길이(과대입력 방어 — RagSearchParam.from 절단 가드와 동일값). */
    private static final int MAX_DERIVE_QUERY_LEN = 2000;

    /** 관리자 보완 설명 최대 길이(SUPP_DESC varchar(2000) 컬럼 절단 방어). */
    private static final int MAX_SUPP_DESC_LEN = 2000;

    private static final String ENDPOINT_CHAT = "riskai01/chat-image";
    private static final String ENDPOINT_DERIVE = "riskai01/derive";
    /** verbatim 선택 전용 호출 감사 엔드포인트(v3.9). */
    private static final String ENDPOINT_DERIVE_SELECT = "riskai01/derive-select";

    /** ★하드가드 #4 fail-closed 기준: 생성 호출(그라운딩)에는 track 이 명시적으로 recompose 인 청크만 투입. */
    private static final String RECOMPOSE_TRACK = "recompose";

    /** verbatim 트랙(라이선스상 변경금지 — 생성 호출 미투입. v3.9: 선택 전용 호출 입력만 허용, 하드가드 #4 개정 참조). */
    private static final String VERBATIM_TRACK = "verbatim";

    /** derive 검색 topK(그라운딩 개선 D — 5→10 확대. LLM 컨텍스트 투입은 별도 캡으로 제한). */
    private static final int DERIVE_SEARCH_TOP_K = 10;

    /** LLM 컨텍스트에 투입하는 recompose 청크 상한(프롬프트 길이 방어 — 기존 5건 유지). */
    private static final int MAX_RECOMPOSE_CONTEXT = 5;

    /** 참고 원문(verbatim 패스스루) 최대 건수(그라운딩 개선 C — v3.9 선택 재랭킹 도입과 함께 3→5 상향). */
    private static final int MAX_VERBATIM_REFS = 5;

    /** verbatim 선택 후보 검색 topK(중복 제거로 후보가 줄어들 것을 감안해 넓게 — Ai01Service maxTopK 클램프 내). */
    private static final int VERBATIM_CANDIDATE_TOP_K = 20;

    /**
     * 법령 신뢰등급 값(prafta-062). 법령 조문은 track=verbatim + data_reliability='법령' 으로 적재되어
     * 기존 verbatim 경로(selectVerbatimRefs)에 SIF 사례와 섞여 들어오므로 부정 필터로 배제하고(D5),
     * 법령 전용 검색(searchLawTrack)에서는 긍정 필터 축으로 사용한다.
     */
    private static final String LAW_RELIABILITY = "법령";

    /** 법령 전용 검색 topK(prafta-062 — maxTopK=20 이내). */
    private static final int LAW_SEARCH_TOP_K = 5;

    /**
     * 법령 사후 매핑 대상 유해요인 상한(prafta-062 배포 D). EmbeddingClient 는 단건 전용이라
     * 유해요인 수만큼 TEI 왕복이 추가되므로 캡으로 지연을 방어한다(계측 로그로 관측 후 조정).
     */
    private static final int MAX_LAW_MAP_HAZARDS = 5;

    /** 유해요인 1건당 첨부 조문 상한(prafta-062 배포 D). */
    private static final int MAX_LAW_REFS_PER_HAZARD = 2;

    /** 라인 프로토콜 각주 마커 "[n]" 추출 정규식(v3.6 — parseDerive/extractMarkers). */
    private static final Pattern MARKER_PATTERN = Pattern.compile("\\[(\\d+)\\]");

    /**
     * 팝업 저장 시 INIT_DESC 뒤에 덧붙는 AI 반영 블록의 시작 마커(WEB_003 저장 액션 —
     * FE RiskAssessInfo.vue 의 AI_HAZARD_MARKER 와 동일 문자열 유지 필수).
     */
    private static final String AI_APPENDIX_MARKER = "[AI분석 유해요인]";

    /** verbatim 선택 응답의 번호 추출 정규식(v3.9 — 숫자 외 텍스트는 전부 폐기). */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    /* prafta-062 배포 D(D9 확정): 법령 조문 첨부 도입에 따른 면책 보강 — 긴 버전. */
    private static final String DISCLAIMER =
        "본 내용은 AI 가 제시한 참고 근거이며, 최종 판단은 담당자가 수행합니다. "
        + "첨부된 법령 조문은 원문을 그대로 인용한 것으로 법률 자문이 아니며, "
        + "개정 여부·시행일은 국가법령정보센터에서 확인해 주세요.";

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    /** 관리자 추가 이미지 캡: 최대 장수(요청단발, v3). */
    private static final int MAX_ADMIN_IMAGES = 2;

    /** 관리자 추가 이미지 캡: 장당 디코드 최대 바이트(3MB, v3). */
    private static final int MAX_ADMIN_IMAGE_BYTES = 3 * 1024 * 1024;

    /** 관리자 추가 이미지 허용 mediaType(v3 — ImagePart 화이트리스트와 동일). */
    private static final Set<String> ALLOWED_ADMIN_IMAGE_TYPES =
        Set.of("image/jpeg", "image/png", "image/webp");

    /** kickoff(자동 첫 질의)로 영속되는 숨김 user 턴 텍스트(v3 — FE 미표시). */
    private static final String KICKOFF_USER_MESSAGE =
        "첨부된 사진이 무엇인지 판단하고, '…로 보입니다. 맞습니까?' 형식의 확인 질문을 해 주세요.";

    /** Yes 확정 시 이력에 남기는 user 발화(v3 confirm-image, LLM 미호출). */
    private static final String CONFIRM_YES_MESSAGE = "예, 맞습니다.";

    /** 이미지 확정 루프(VLM) 시스템 프롬프트 — "…로 보입니다. 맞습니까?" 확인 질의형(v3). */
    private static final String IMG_CONFIRM_SYSTEM_PROMPT =
          "당신은 산업안전 위험성평가 보조자다. 관리자가 첨부한 작업 현장 사진이 무엇인지 판독해 확인 질문을 한다.\n"
        + "1. 사진에서 실제로 보이는 것만 근거로 판단한다(보이지 않는 사실을 지어내지 않는다).\n"
        + "2. 판독 결과를 간결한 한국어로 서술하고, 반드시 \"해당 이미지는 …로 보입니다. 맞습니까?\" 형식의\n"
        + "   확인 질문으로 끝맺는다.\n"
        + "3. 관리자가 정정 설명이나 추가 이미지를 주면 이를 반영해 판단을 수정하고, 다시 같은 형식으로 재질의한다.\n"
        + "4. 유해요인·개선안을 나열하지 않는다. 이 대화의 목적은 '사진이 무엇인지' 확정하는 것이다.\n"
        + "5. 이전 대화 맥락을 이어서 일관되게 답한다.\n"
        + "출력은 간결한 서술 텍스트만 반환하며 마지막 문장은 반드시 확인 질문이다. 머리말/맺음말/JSON 은 포함하지 않는다.";

    /**
     * 도출(그라운딩) 시스템 프롬프트 — recompose 청크 근거로만 유해요인별 개선안 그룹 도출 + 각주(v3).
     * ★출력 형식 = 라인 프로토콜(v3.6): HCX-005 가 단순화한 JSON 스키마조차 중첩 괄호 불일치를
     *   상습 반복해(hazards 배열 미폐합 등) JSON 을 전면 폐기하고 줄 단위 프로토콜로 전환.
     *   citations 는 서버가 [n] 마커→markerToHit 대조로 결정적 조립(v3.5 유지).
     */
    private static final String DERIVE_GROUNDED_SYSTEM_PROMPT =
          "당신은 산업안전 위험성평가 보조자다. 아래 [근거 청크]에 있는 내용만으로 유해·위험요인과 개선안(감소대책)을 도출한다.\n"
        + "★[관리자 우선 원칙] [유해요인명]·[유해요인 설명]은 근로자(신고자)가 입력한 내용으로 부정확할 수 있다.\n"
        + "   [관리자 보완 설명]과 [이미지 확정 대화]는 관리자가 확인·확정한 내용이다. 두 입력이 서로 다르거나\n"
        + "   충돌하면(예: 근로자는 'Sorter'라 했으나 [이미지 확정 대화]에서 '컨베이어 벨트'로 확정) 반드시 관리자의\n"
        + "   확정 내용을 정답으로 삼고, 근로자가 쓴 설비명·대상·설명은 폐기한다. 확정된 대상 기준으로 유해요인·개선안을 도출한다.\n"
        + "1. 청크에 없는 사실·수치·대책을 지어내지 않는다.\n"
        + "2. 각 항목(유해요인/개선안) 줄 끝에 근거 청크 번호를 [n] 형식으로 표기한다.\n"
        + "   [근거 청크]에 실제 존재하는 번호만 표기한다.\n"
        + "3. 근거 청크가 요청과 부분적으로라도 관련되면 적극 활용해 도출한다. 모든 청크가 요청과 전혀\n"
        + "   무관한 경우에만 ABSTAIN: Y 만 출력한다.\n"
        + "4. 요청의 유해요인명을 그대로 반복하는 데 그치지 말고, [근거 청크]의 사고 사례(사고개요)에서\n"
        + "   이 작업·설비에 적용될 수 있는 유해·위험요인을 복수로 폭넓게 도출한다. 각 유해요인은\n"
        + "   근거 청크에서 확인 가능한 것이어야 하며 [n] 으로 근거를 표시한다.\n"
        + "5. 근거의 신뢰등급을 반영한다: '자율신고형'은 단정하지 말고 약하게 인용한다.\n"
        + "6. 각 유해요인(H: 줄)의 M: 줄에는 그 유해요인에 대한 개선안만 담는다. 유해요인마다 1개 이상의 개선안을 제시한다.\n"
        + "7. 이 도출은 참고용이며 최종 판단은 담당자가 한다.\n"
        + "출력 형식(다른 텍스트·머리말·맺음말·JSON 금지):\n"
        + "첫 줄에 ABSTAIN: N (근거가 전혀 무관해 도출 불가면 ABSTAIN: Y 만 출력)\n"
        + "이후 유해요인마다:\n"
        + "H: <유해요인 서술> [n][m]\n"
        + "M: <해당 유해요인의 개선안 서술> [n]\n"
        + "M: <다른 개선안 서술> [n]\n"
        + "(H: 줄 다음의 M: 줄들이 그 유해요인의 개선안이다. [n]은 근거 청크 번호다.)";

    /**
     * 도출(자유생성) 시스템 프롬프트 — 코퍼스 근거부재 시 일반 지식으로 그룹 생성(각주 없음, abstained=true)(v3).
     * ★출력 형식 = 라인 프로토콜(v3.6 — 그라운딩 프롬프트와 동일 사유로 JSON 폐기. [n] 각주 표기 금지).
     */
    private static final String DERIVE_FREE_SYSTEM_PROMPT =
          "당신은 산업안전 위험성평가 보조자다. 제공된 위험성평가 요청 내용을 바탕으로 유해·위험요인과 개선안(감소대책)을\n"
        + "일반적인 산업안전 지식으로 제시한다. 근거 코퍼스가 없으므로 [n] 형식의 각주 표기를 하지 않는다.\n"
        + "★[관리자 우선 원칙] [유해요인명]·[유해요인 설명]은 근로자(신고자)가 입력한 내용으로 부정확할 수 있다.\n"
        + "[관리자 보완 설명]과 [이미지 확정 대화]는 관리자가 확인·확정한 내용이다. 두 입력이 충돌하면\n"
        + "(예: 근로자는 'Sorter'라 했으나 [이미지 확정 대화]에서 '컨베이어 벨트'로 확정) 반드시 관리자의 확정 내용을\n"
        + "정답으로 삼고, 근로자가 쓴 설비명·대상·설명은 폐기한 뒤 확정된 대상 기준으로 도출한다.\n"
        + "요청의 유해요인명을 그대로 반복하는 데 그치지 말고, 이 작업·설비에 적용될 수 있는 유해·위험요인을\n"
        + "복수로 폭넓게 도출한다. 유해요인은 서로 다른 관점에서 가능한 한 3개 이상 도출한다\n"
        + "(요청의 유해요인명 반복 1건으로 끝내지 않는다). 각 유해요인(H: 줄)의 M: 줄에는 그 유해요인에 대한\n"
        + "개선안만 담고, 유해요인마다 1개 이상의 개선안을 제시한다. 참고용이며 최종 판단은 담당자가 한다.\n"
        + "출력 형식(다른 텍스트·머리말·맺음말·JSON 금지):\n"
        + "첫 줄에 ABSTAIN: Y\n"
        + "이후 유해요인마다:\n"
        + "H: <유해요인 서술>\n"
        + "M: <해당 유해요인의 개선안 서술>\n"
        + "M: <다른 개선안 서술>\n"
        + "(H: 줄 다음의 M: 줄들이 그 유해요인의 개선안이다. [n] 등 각주 표기는 금지)";

    /**
     * verbatim(3유형) 참고 원문 선택 전용 시스템 프롬프트(v3.9 — 선택 전용 LLM 재랭킹).
     *
     * <p>★하드가드 #4 개정(v3.9): verbatim 원문은 생성 호출(그라운딩/자유생성)에는 절대 미투입.
     *    단 이 "선택 전용 호출"에는 입력 가능 — 출력이 번호로 제한되고 서버가 숫자 외 출력을
     *    전부 폐기하며 화면은 서버가 DB 원문을 그대로 렌더하므로, LLM 출력물에 3유형 텍스트가
     *    한 글자도 포함되지 않아 원문 변형 배포가 구조적으로 불가능하다.
     */
    private static final String VERBATIM_SELECT_SYSTEM_PROMPT =
          "당신은 산업안전 위험성평가 보조자다. 아래 [후보 원문] 중 [위험성평가 요청]과 관련 있는 항목을 고른다.\n"
        + "1. 출력은 \"V: n, m\" 형식 한 줄만 반환한다(번호는 관련성 높은 순, 최대 5개).\n"
        + "2. 관련 있는 항목이 없으면 \"V: 없음\" 만 반환한다.\n"
        + "3. 후보의 문장을 출력에 옮겨 쓰지 않는다. 번호 외 다른 텍스트를 출력하지 않는다.";

    // ==================================================================
    // 1) 기존 결과/대화 로드
    // ==================================================================

    @Override
    public RiskAiDerivationResponse getDerivation(RiskAiParam param) {
        log.info("AI 도출 결과 조회 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        validateKeys(param);
        assertSiteAccess(param);

        // 대상 평가건 존재 확인(회사+사업장 스코프). 없으면 404(존재 미노출).
        loadAssessmentOr404(param);

        RiskAiDerivationRow row = riskAi01Mapper.selectDerivation(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());
        return buildResponse(row);
    }

    // ==================================================================
    // 1-2) 관리자 보완 설명 저장(직접입력, blur 자동저장 — v2.1)
    // ==================================================================

    @Override
    public RiskAiDerivationResponse saveSupplement(RiskAiParam param) {
        log.info("AI 보완 설명 저장 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        validateKeys(param);
        assertSiteAccess(param);
        loadAssessmentOr404(param);

        // 도출 행 보장 후 보완 설명 저장(공백/전체삭제도 그대로 반영 → 관리자 자유 편집 허용).
        riskAi01Mapper.insertDerivationIfAbsent(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(), param.gvUserCd());
        riskAi01Mapper.updateSuppDesc(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(),
            clampSupp(param.suppDesc()), param.gvUserCd());

        RiskAiDerivationRow row = riskAi01Mapper.selectDerivation(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());
        return buildResponse(row);
    }

    // ==================================================================
    // 2) 이미지 확정 루프 채팅(VLM, ★캡 없음, 이미지 매 요청 재부착 + v3 kickoff/adminImages)
    // ==================================================================

    @Override
    public RiskAiDerivationResponse chatImage(RiskAiParam param) {
        boolean kickoff = Boolean.TRUE.equals(param.kickoff());
        log.info("AI 이미지확정 채팅 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}, kickoff={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(), kickoff);

        validateKeys(param);
        assertSiteAccess(param);

        RiskAssessmentAiSource source = loadAssessmentOr404(param);

        // 사전거부 ① 이미지 미첨부 → 400_002.
        if (!StringUtils.hasText(source.initFileMgmtCd())) {
            throw new ApiException(AiErrorCode.AI_400_002);
        }
        // 사전거부 ② 사용자 메시지 공백 → 400_001 (★kickoff 는 userMessage 불요 — 스킵).
        if (!kickoff && !StringUtils.hasText(param.userMessage())) {
            throw new ApiException(AiErrorCode.AI_400_001);
        }
        // 사전거부 ③ 관리자 추가 이미지 검증(개수/형식/디코드 용량) → 위반 시 400_004.
        List<ImagePart> adminImages = validateAdminImages(param.adminImages());
        // 사전거부 ④ 게이트 OFF → 503.
        if (!llmAnswerClient.isEnabled()) {
            throw new ApiException(AiErrorCode.AI_503_001);
        }

        // ★이미지 매 요청 재로드(회사 스코프). DB/디스크에 없으면 404.
        ImageBytesResult image = fileService.loadImageBytes(
            new FileReadQuery(param.gvCmpnyCd(), source.initFileMgmtCd()));
        if (image == null || image.data() == null || image.data().length == 0) {
            throw new ApiException(AiErrorCode.AI_404_001);
        }

        // 도출 행 보장 후, 보완 설명(있으면) 저장 → 기존 대화 이력 로드.
        riskAi01Mapper.insertDerivationIfAbsent(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(), param.gvUserCd());
        if (StringUtils.hasText(param.suppDesc())) {
            riskAi01Mapper.updateSuppDesc(
                param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(),
                clampSupp(param.suppDesc()), param.gvUserCd());
        }
        RiskAiDerivationRow before = riskAi01Mapper.selectDerivation(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        List<ChatTurn> updated = new ArrayList<>(parseChatTurns(before == null ? null : before.imgChatJson()));

        if (kickoff) {
            // ★멱등: assistant 확인 질의가 이미 있으면 LLM 미호출·현재 상태 반환.
            if (hasAssistantTurn(updated)) {
                log.info("AI 이미지확정 kickoff 멱등 스킵(assistant 발화 존재) - turns={}", updated.size());
                return buildResponse(before);
            }
            // 숨김 user 턴(kickoff 지시문)을 이력에 영속 append. FE 는 hidden 턴을 렌더하지 않는다.
            updated.add(new ChatTurn(ROLE_USER, KICKOFF_USER_MESSAGE, Boolean.TRUE));
        } else {
            // 관리자 정정 메시지. 추가 이미지가 있으면 턴 텍스트에 접두로 흔적을 남긴다(이미지 자체는 미영속).
            String text = param.userMessage().trim();
            if (!adminImages.isEmpty()) {
                text = "(이미지 " + adminImages.size() + "장 첨부) " + text;
            }
            updated.add(new ChatTurn(ROLE_USER, text, null));
        }

        // ★재구성 turns 의 "첫 user 턴"에 디스크 로드 이미지를 재부착 + 관리자 추가 이미지는 "이번(마지막) user 턴"에만 부착.
        //   HCX 요청당 이미지 제약(5장)은 캡(초기1 + 관리자2 = 3)으로 자동 준수.
        String base64 = Base64.getEncoder().encodeToString(image.data());
        List<HcxTurn> turns = toHcxTurns(updated, new ImagePart(base64, image.mediaType()), adminImages);

        // ★이미지 분석 컨텍스트: 시스템 프롬프트 뒤에 근로자 입력 + 관리자 보완 설명을 요청빌드 시점에만 주입
        //   (IMG_CHAT_JSON 엔 미저장). 이미지가 무엇인지 확정하는 데 근로자/관리자 맥락을 함께 참고하게 한다.
        String suppDesc = resolveSuppDesc(param, before);
        String system = buildImageContext(source, suppDesc);

        // 회사 월간 AI 토큰 쿼터 게이트(플랫폼-AI-토큰쿼터 §2-4) — 호출 직전 1회, 소진 시 AI_429_001.
        aiQuotaService.checkOrThrow(param.gvCmpnyCd());

        // 멀티턴 호출(실패 시 client 가 예외 → 저장 없이 전파. ★캡/롤백 없음).
        LlmRawResponse raw = llmAnswerClient.chat(system, turns);
        // ★쿼터 사용량 누적 — raw 수신 즉시(refusal 여부와 무관).
        aiQuotaService.record(param.gvCmpnyCd(), raw.inputTokens(), raw.outputTokens());

        // assistant 응답 append(비-refusal + 텍스트 유효 시에만).
        if (!raw.refusal()) {
            String answer = safeTrim(raw.combinedText());
            if (StringUtils.hasText(answer)) {
                updated.add(new ChatTurn(ROLE_ASSISTANT, answer, null));
            }
        }

        // 대화 이력 저장(텍스트만 — base64 미저장).
        riskAi01Mapper.updateImgChatJson(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(),
            toJson(updated), param.gvUserCd());

        // 감사·과금(best-effort — LLM 호출이 있었던 경우만 도달).
        int queryLen = kickoff ? KICKOFF_USER_MESSAGE.length() : param.userMessage().length();
        recordCall(param, ENDPOINT_CHAT, raw, queryLen, List.of(), raw.refusal());

        RiskAiDerivationRow row = riskAi01Mapper.selectDerivation(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());
        log.info("AI 이미지확정 채팅 완료 - refusal={}, turns={}, adminImages={}",
            raw.refusal(), updated.size(), adminImages.size());
        return buildResponse(row);
    }

    // ==================================================================
    // 2-2) 이미지 이해 확정(Yes) — LLM 미호출(v3)
    // ==================================================================

    @Override
    public RiskAiDerivationResponse confirmImage(RiskAiParam param) {
        log.info("AI 이미지 이해 확정 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        validateKeys(param);
        assertSiteAccess(param);

        RiskAssessmentAiSource source = loadAssessmentOr404(param);

        // 사전거부 ① 이미지 미첨부 → 400_002(확정 대상 이미지 자체가 없음).
        if (!StringUtils.hasText(source.initFileMgmtCd())) {
            throw new ApiException(AiErrorCode.AI_400_002);
        }

        riskAi01Mapper.insertDerivationIfAbsent(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(), param.gvUserCd());
        RiskAiDerivationRow before = riskAi01Mapper.selectDerivation(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        List<ChatTurn> updated = new ArrayList<>(parseChatTurns(before == null ? null : before.imgChatJson()));

        // 사전거부 ② 확정할 assistant 확인 질의가 없음 → 400_005.
        if (!hasAssistantTurn(updated)) {
            throw new ApiException(AiErrorCode.AI_400_005);
        }

        // ★멱등 보정(v3 뒤로가기): 이미 확정(IMG_CONFIRMED='Y')된 행이라도 마지막 visible 턴이
        //   assistant(=뒤로가기 후 재질의로 생긴 미응답 확인 질의)면 재확정 발화를 새로 기록한다.
        //   미응답 질의가 없는 확정 행이면 조기 반환(더블클릭 등 중복 호출 시 턴 중복 누적 방지).
        if (before != null && "Y".equals(before.imgConfirmed()) && !isLastVisibleAssistant(updated)) {
            log.info("AI 이미지 이해 확정 멱등 스킵(이미 확정됨, 미응답 질의 없음)");
            return buildResponse(before);
        }

        // Yes 확정: visible user 턴 append → 이력 저장 → IMG_CONFIRMED='Y'. ★LLM 미호출, recordCall 불요.
        updated.add(new ChatTurn(ROLE_USER, CONFIRM_YES_MESSAGE, null));
        riskAi01Mapper.updateImgChatJson(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(),
            toJson(updated), param.gvUserCd());
        riskAi01Mapper.updateImgConfirmed(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(),
            "Y", param.gvUserCd());

        RiskAiDerivationRow row = riskAi01Mapper.selectDerivation(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());
        log.info("AI 이미지 이해 확정 완료 - turns={}", updated.size());
        return buildResponse(row);
    }

    // ==================================================================
    // 3) 유해요인·개선안 도출(RAG + LLM, ★캡 없음)
    // ==================================================================

    @Override
    public RiskAiDerivationResponse derive(RiskAiParam param) {
        log.info("AI 유해요인·개선안 도출 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        validateKeys(param);
        assertSiteAccess(param);

        RiskAssessmentAiSource source = loadAssessmentOr404(param);
        RiskAiDerivationRow before = riskAi01Mapper.selectDerivation(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        // ★순서 강제 해제(v2.1): 이미지 분석 선행 없이도 도출 가능(관리자 선택형).
        //   이미지가 있는데 분석(VLM 답변)이 없으면 이미지는 도출에 반영되지 않으나 차단하지 않는다
        //   (FE 에서 빨간 경고문으로만 안내). suppDesc 는 근로자 입력에 더해 도출 질의에 투입한다.

        // 질의 = 구조화 값(공정명/위험성분류/유해요인명/유해요인설명) + 관리자 보완 설명
        //       + 이미지 확정 대화 전체(visible user+assistant — v3.7: 관리자 채팅 발화가 곧 관리자 의견).
        String suppDesc = resolveSuppDesc(param, before);
        String query = buildDeriveQuery(source, before, suppDesc);
        if (!StringUtils.hasText(query)) {
            throw new ApiException(AiErrorCode.AI_400_001);
        }
        // 과대입력 방어(RagSearchParam.from 절단 가드 우회 방지): 검색·LLM 투입 전 길이 클램프.
        if (query.length() > MAX_DERIVE_QUERY_LEN) {
            query = query.substring(0, MAX_DERIVE_QUERY_LEN);
        }

        // 사전거부 게이트 OFF → 503.
        if (!llmAnswerClient.isEnabled()) {
            throw new ApiException(AiErrorCode.AI_503_001);
        }
        // 회사 월간 AI 토큰 쿼터 게이트(플랫폼-AI-토큰쿼터 §2-4) — 진입부 1회.
        //   derive 체인 최대 3회 + verbatim 선택 호출(selectVerbatimRefs)도 이 체크로 커버(소프트 리밋 명세 §5).
        aiQuotaService.checkOrThrow(param.gvCmpnyCd());

        // RAG 검색(코퍼스 필수). ★그라운딩 개선 D: topK 5→10 확대(회수율 개선).
        //   LLM 컨텍스트에는 아래 잘라내기에서 recompose 상위 5건만 투입한다(프롬프트 길이 방어).
        // prafta-062: reliabilityNotIn=null(부정 필터 미적용) — 사례 트랙 결과는 종전과 완전 동일(무회귀).
        RagSearchParam searchParam = new RagSearchParam(
            query, DERIVE_SEARCH_TOP_K, null, null, null, null, param.gvUserCd(), param.gvCmpnyCd());
        RagSearchResponse searchResp = ai01Service.search(searchParam);
        List<RagHit> hits = (searchResp.getHits() == null) ? List.of() : searchResp.getHits();

        // ★track 물리 분리(하드가드 #4, fail-closed): track == recompose 만 생성 호출(그라운딩) 컨텍스트.
        // ★잘라내기: 검색은 topK=10 이지만 LLM 투입 recompose 는 유사도 상위 5건(MAX_RECOMPOSE_CONTEXT)
        //   까지만 채택한다(hits 는 유사도순 정렬 전제).
        //   verbatim 은 v3.9 부터 메인 검색이 아니라 아래 "전용 검색 + 선택 전용 호출"로 별도 선정.
        // ★그라운딩 개선(유사도 임계값): 코퍼스에 관련 자료가 없으면 벡터 검색은 "가장 덜 먼" 무관한
        //   청크(예: 제조업 질의에 건설 컨베이어/지게차)를 반환한다. minScore 미만은 근거로 채택하지
        //   않아, 무관한 청크를 억지 근거로 삼는 것을 막는다. 전부 미달이면 recompose 가 비어
        //   아래 "코퍼스 근거부재 → 자유생성(ABSTAINED)" 경로로 폴백한다(정직한 저신뢰 응답).
        double minScore = aiProperties.getSearch().getDeriveMinScore();
        double topScore = hits.isEmpty() ? Double.NaN : hits.get(0).getScore();
        int recomposeTrackCnt = 0;
        List<RagHit> recompose = new ArrayList<>();
        for (RagHit h : hits) {
            if (!RECOMPOSE_TRACK.equals(h.getTrack())) {
                continue;
            }
            recomposeTrackCnt++;
            if (h.getScore() >= minScore && recompose.size() < MAX_RECOMPOSE_CONTEXT) {
                recompose.add(h);
            }
        }
        // ★관측 로그(임계값 튜닝 근거): 실 코퍼스의 최상위 점수와 임계값 통과 건수를 남긴다.
        //   실환경에서 이 로그의 topScore 분포를 보고 deriveMinScore 를 조정한다.
        log.info("RAG 그라운딩 판정 - 질의길이={}, recompose후보={}, 최상위점수={}, 임계값={}, 채택={}건{}",
                query.length(), recomposeTrackCnt,
                Double.isNaN(topScore) ? "N/A" : String.format("%.4f", topScore),
                String.format("%.2f", minScore), recompose.size(),
                recompose.isEmpty() ? " → 근거부재(자유생성 ABSTAINED)" : "");

        // ★v3.9 verbatim 참고 원문 선정: 전용 검색(track=verbatim) 후 "선택 전용 LLM" 재랭킹.
        //   그라운딩 호출 전에 수행하되, 어떤 실패도 derive 본류에 영향 없게 내부에서 격리·폴백한다.
        //   화면 표시는 서버가 DB 원문 그대로(원문 무변경 패스스루) — 하드가드 #4 개정 주석 참조.
        List<VerbatimRefItem> verbatimRefs = selectVerbatimRefs(param, query);

        // 도출 행 보장(★캡 증가 없음) + 보완 설명(있으면) 동반저장.
        riskAi01Mapper.insertDerivationIfAbsent(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(), param.gvUserCd());
        if (StringUtils.hasText(param.suppDesc())) {
            riskAi01Mapper.updateSuppDesc(
                param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(),
                clampSupp(param.suppDesc()), param.gvUserCd());
        }

        List<DerivedHazardGroup> hazards;
        List<CitationItem> citations;
        boolean abstained;
        List<String> usedChunkIds = new ArrayList<>();
        LlmRawResponse raw;

        if (recompose.isEmpty()) {
            // 코퍼스 근거부재 → 자유생성(각주 없음, ABSTAINED=Y).
            raw = llmAnswerClient.answer(DERIVE_FREE_SYSTEM_PROMPT, "[위험성평가 요청]\n" + query);
            // ★쿼터 사용량 누적 — raw 수신 즉시(기존 recordCall 감사와 병행 — 대체 아님).
            aiQuotaService.record(param.gvCmpnyCd(), raw.inputTokens(), raw.outputTokens());
            DeriveLlmResult parsed = raw.refusal() ? null : parseDerive(raw.combinedText());
            hazards = (parsed == null) ? List.of() : stripHazardMarkers(parsed.hazards());
            citations = List.of();
            abstained = true;
        } else {
            // 그라운딩 도출: recompose 컨텍스트. citations 는 LLM 출력 스키마에서 제거했고
            //   서버가 markers→markerToHit 대조로 결정적으로 조립한다(HCX JSON 안정화).
            Map<Integer, RagHit> markerToHit = new LinkedHashMap<>();
            String context = buildGroundedContext(query, recompose, markerToHit);
            raw = llmAnswerClient.answer(DERIVE_GROUNDED_SYSTEM_PROMPT, context);
            // ★쿼터 사용량 누적 — raw 수신 즉시.
            aiQuotaService.record(param.gvCmpnyCd(), raw.inputTokens(), raw.outputTokens());
            DeriveLlmResult parsed = raw.refusal() ? null : parseDerive(raw.combinedText());

            // ★파싱 실패(HCX 확률적 JSON 문법 오류) 시 동일 프롬프트로 1회 재시도.
            //   실패한 1차 호출은 이 시점에 inline 감사 기록(usedChunkIds 없음, abstained=true)
            //   — 실제 HCX 호출 건마다 기록 누락 없음(최대 3회: 1차 실패 → 재시도 실패 → 자유생성).
            if (parsed == null) {
                recordCall(param, ENDPOINT_DERIVE, raw, query.length(), List.of(), true);
                log.warn("그라운딩 응답 파싱 실패 - 동일 프롬프트로 1회 재시도");
                raw = llmAnswerClient.answer(DERIVE_GROUNDED_SYSTEM_PROMPT, context);
                // ★쿼터 사용량 누적 — 재시도 호출도 raw 수신 즉시.
                aiQuotaService.record(param.gvCmpnyCd(), raw.inputTokens(), raw.outputTokens());
                parsed = raw.refusal() ? null : parseDerive(raw.combinedText());
            }

            // 재시도도 실패/abstain/유효 hazard 0건(방어 파싱이 스키마 위반 요소를 걸러낸 경우 포함)이면 폴백.
            if (parsed == null || parsed.abstained() || parsed.hazards().isEmpty()) {
                // ★그라운딩 폴백(v3 사용자 확정): 코퍼스 히트는 있었으나 활용 가능한 결과가 없는 경우,
                //   빈 결과로 끝내지 않고 자유생성(각주 없음)으로 폴백한다.
                //   LLM 실호출이 추가되므로 직전 그라운딩 호출 감사를 먼저 기록(usedChunkIds 없음).
                recordCall(param, ENDPOINT_DERIVE, raw, query.length(), List.of(), true);

                raw = llmAnswerClient.answer(DERIVE_FREE_SYSTEM_PROMPT, "[위험성평가 요청]\n" + query);
                // ★쿼터 사용량 누적 — 폴백 호출도 raw 수신 즉시.
                aiQuotaService.record(param.gvCmpnyCd(), raw.inputTokens(), raw.outputTokens());
                // parseDerive 는 파싱 불가 시 예외 대신 null 반환 — 폴백 파싱 실패는 빈 결과로 종결.
                DeriveLlmResult fallback = raw.refusal() ? null : parseDerive(raw.combinedText());
                hazards = (fallback == null) ? List.of() : stripHazardMarkers(fallback.hazards());
                citations = List.of();
                abstained = true;
            } else {
                // ★서버 결정적 조립: hazards/measures 의 markers 를 전수 수집해 [근거 청크]에 실재하는
                //   마커만 유효 채택 → citations/usedChunkIds 서버 조립(환각 마커 차단 유지).
                //   markerToHit 에 없는 마커는 무효 → filterHazardMarkers 로 함께 제거.
                Set<String> validMarkers = new LinkedHashSet<>();
                citations = buildCitationsFromMarkers(parsed.hazards(), markerToHit, validMarkers, usedChunkIds);
                hazards = filterHazardMarkers(parsed.hazards(), validMarkers);
                abstained = false;
            }
        }

        // prafta-062 배포 D(062-07): 유해요인별 법령 사후 매핑 — hazards/citations/abstained 확정 이후
        //   확정된 유해요인에 조문을 additive 로 첨부만 한다(★유해요인 추가/삭제/재정렬 금지 = 핵심 회귀 축).
        //   ABSTAINED(자유생성) 결과에도 동일 적용(D7=(a) — 조문은 LLM 산출물이 아닌 서버 첨부라 abstained 와 독립).
        hazards = attachLawRefs(param, source, hazards);

        // 결과 저장(v3: HAZARD_JSON 그룹 구조 + CITATION_JSON + VERBATIM_JSON + ABSTAINED.
        //   MEASURE_JSON 은 NULL 클리어. VERBATIM_JSON 은 재열람 복원용 — 개선 C.
        //   prafta-062: lawRefs 는 HAZARD_JSON(네이티브 json 타입 — 64KB 한도 없음, D8=(a))에 중첩 저장).
        riskAi01Mapper.updateDeriveResult(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(),
            toJson(hazards), toJson(citations), toJson(verbatimRefs),
            abstained ? "Y" : "N", param.gvUserCd());

        // 감사·과금(best-effort).
        recordCall(param, ENDPOINT_DERIVE, raw, query.length(), usedChunkIds, abstained);

        RiskAiDerivationRow row = riskAi01Mapper.selectDerivation(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());
        log.info("AI 도출 완료 - abstained={}, hazards={}, citations={}, verbatimRefs={}",
            abstained, hazards.size(), citations.size(), verbatimRefs.size());
        return buildResponse(row);
    }

    // ==================================================================
    // 4) 초기화(처음부터 다시) — LLM 미호출
    // ==================================================================

    @Override
    public RiskAiDerivationResponse reset(RiskAiParam param) {
        log.info("AI 도출 초기화 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        validateKeys(param);
        assertSiteAccess(param);
        loadAssessmentOr404(param);

        // 도출 행 DELETE: 대화이력/도출결과/보완설명(SUPP_DESC 포함) 전체 삭제. 행 없으면 0건(무해).
        //   ★LLM 미호출 — recordCall 불요.
        int deleted = riskAi01Mapper.deleteDerivation(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        log.info("AI 도출 초기화 완료 - deleted={}", deleted);
        // 초기 상태 반환(buildResponse(null) = 빈 대화/결과 — 기존 미생성 행 응답 패턴 재사용).
        return buildResponse(null);
    }

    // ==================================================================
    // 5) 미저장 정리(commit-on-save) — LLM 미호출
    // ==================================================================

    @Override
    public RiskAiDerivationResponse discardUnsaved(RiskAiParam param) {
        log.info("AI 도출 미저장 정리 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        validateKeys(param);
        assertSiteAccess(param);
        loadAssessmentOr404(param);

        // 미확정(SAVED_YN='N') 행만 삭제. 확정(Y) 행은 보존한다. ★LLM 미호출 — recordCall 불요.
        int deleted = riskAi01Mapper.deleteUnsavedDerivation(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        // 삭제 후 현재 행을 재조회해 반환(확정분이 남아 있으면 그 상태를 그대로 돌려준다).
        RiskAiDerivationRow row = riskAi01Mapper.selectDerivation(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        log.info("AI 도출 미저장 정리 완료 - deleted={}", deleted);
        return buildResponse(row);
    }

    // ==================================================================
    // 6) 저장 확정(commit-on-save) — LLM 미호출
    // ==================================================================

    @Override
    public RiskAiDerivationResponse commit(RiskAiParam param) {
        log.info("AI 도출 저장 확정 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        validateKeys(param);
        assertSiteAccess(param);
        loadAssessmentOr404(param);

        // SAVED_YN='Y' 확정. 행이 없으면 0건 갱신으로 무해. ★LLM 미호출 — recordCall 불요.
        int updated = riskAi01Mapper.updateSavedYn(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(),
            "Y", param.gvUserCd());

        RiskAiDerivationRow row = riskAi01Mapper.selectDerivation(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        log.info("AI 도출 저장 확정 완료 - updated={}", updated);
        return buildResponse(row);
    }

    // ==================================================================
    // 내부 헬퍼 — 권한/존재
    // ==================================================================

    /** 평가건 스코프 키(3축) 필수값 검증. */
    private void validateKeys(RiskAiParam param) {
        if (!StringUtils.hasText(param.siteCd())
                || !StringUtils.hasText(param.processCd())
                || !StringUtils.hasText(param.assessmentCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
    }

    /**
     * 사업장(siteCd) 접근 권한 검증(risklink01 assertSiteAccess 패턴 복제).
     * 전사 권한(master/hr)은 통과, 그 외는 tb_user_site_auth(USE_YN='Y') 매핑 보유 시에만 통과.
     * ★역할(safe 등) 제한은 추가하지 않는다(도메인 관례=사업장 통제). 권한 없으면 존재 미노출로 404 처리.
     */
    private void assertSiteAccess(RiskAiParam param) {
        if (AuthRoleUtils.isManager(param.gvAuthCd())) {
            return;
        }
        if (!StringUtils.hasText(param.siteCd())
                || riskAi01Mapper.countUserSiteAuth(param.gvCmpnyCd(), param.gvUserCd(), param.siteCd()) == 0) {
            log.warn("AI 도출 사업장 권한 없음 - userCd={}, authCd={}, siteCd={}",
                param.gvUserCd(), param.gvAuthCd(), param.siteCd());
            throw new ApiException(AiErrorCode.AI_404_001);
        }
    }

    /** 대상 평가건 조회(회사+사업장 스코프). 없으면 AI_404_001. */
    private RiskAssessmentAiSource loadAssessmentOr404(RiskAiParam param) {
        RiskAssessmentAiSource source = riskAi01Mapper.selectAssessmentForAi(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());
        if (source == null) {
            throw new ApiException(AiErrorCode.AI_404_001);
        }
        return source;
    }

    // ==================================================================
    // 내부 헬퍼 — 대화/프롬프트 조립
    // ==================================================================

    /**
     * ChatTurn 목록 → HcxTurn 목록. ★초기 이미지는 "첫 user 턴"에 재부착하고,
     * 관리자 추가 이미지(요청단발)는 "마지막 user 턴"(=이번 턴)에만 부착한다.
     * 요청당 이미지 제약(5장)은 캡(초기1 + 관리자2 = 3)으로 자동 준수.
     */
    private List<HcxTurn> toHcxTurns(List<ChatTurn> turns, ImagePart initImage, List<ImagePart> adminImages) {
        // 마지막 user 턴 위치(이번 턴) 탐색 — 관리자 추가 이미지 부착 대상.
        int lastUserIdx = -1;
        for (int i = 0; i < turns.size(); i++) {
            ChatTurn t = turns.get(i);
            if (t != null && ROLE_USER.equals(t.role())) {
                lastUserIdx = i;
            }
        }

        List<HcxTurn> out = new ArrayList<>();
        boolean initAttached = false;
        for (int i = 0; i < turns.size(); i++) {
            ChatTurn t = turns.get(i);
            if (t == null || t.role() == null) {
                continue;
            }
            List<ImagePart> images = new ArrayList<>();
            if (!initAttached && ROLE_USER.equals(t.role())) {
                images.add(initImage);
                initAttached = true;
            }
            if (i == lastUserIdx && adminImages != null && !adminImages.isEmpty()) {
                images.addAll(adminImages);
            }
            out.add(new HcxTurn(t.role(), t.text(), images.isEmpty() ? null : images));
        }
        return out;
    }

    /** 이력에 assistant 발화(확인 질의)가 1건 이상 존재하는지. */
    private boolean hasAssistantTurn(List<ChatTurn> turns) {
        for (ChatTurn t : turns) {
            if (t != null && ROLE_ASSISTANT.equals(t.role())) {
                return true;
            }
        }
        return false;
    }

    /** 마지막 visible(hidden 아님) 턴이 assistant(=미응답 확인 질의 존재)인지(v3 뒤로가기 재확정 판별). */
    private boolean isLastVisibleAssistant(List<ChatTurn> turns) {
        for (int i = turns.size() - 1; i >= 0; i--) {
            ChatTurn t = turns.get(i);
            if (t == null || Boolean.TRUE.equals(t.hidden())) {
                continue;
            }
            return ROLE_ASSISTANT.equals(t.role());
        }
        return false;
    }

    /**
     * 관리자 추가 이미지 검증(v3, 요청단발): 개수 ≤ 2, mediaType ∈ {jpeg,png,webp},
     * base64 문자열 길이 선검증(★디코드 전 힙 증폭 방지) + 디코드 성공 + 디코드 크기 ≤ 3MB +
     * 실바이트 매직 시그니처 확인(★mediaType 신고값 위조 방어). 위반 시 AI_400_004.
     * 통과분만 ImagePart 로 변환(영속 없음).
     */
    private List<ImagePart> validateAdminImages(List<RiskAiRequest.AdminImageIn> adminImages) {
        if (adminImages == null || adminImages.isEmpty()) {
            return List.of();
        }
        if (adminImages.size() > MAX_ADMIN_IMAGES) {
            throw new ApiException(AiErrorCode.AI_400_004);
        }
        List<ImagePart> out = new ArrayList<>();
        for (RiskAiRequest.AdminImageIn img : adminImages) {
            if (img == null
                    || !StringUtils.hasText(img.getBase64())
                    || !ALLOWED_ADMIN_IMAGE_TYPES.contains(img.getMediaType())) {
                throw new ApiException(AiErrorCode.AI_400_004);
            }
            // ★디코드 전 base64 문자열 길이 선검증: 3MB 원본의 base64 상한(4/3 배 + 패딩) 초과 시 즉시 거부.
            if (img.getBase64().length() > (MAX_ADMIN_IMAGE_BYTES / 3 * 4 + 4)) {
                throw new ApiException(AiErrorCode.AI_400_004);
            }
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(img.getBase64());
            } catch (IllegalArgumentException e) {
                throw new ApiException(AiErrorCode.AI_400_004);
            }
            if (decoded.length == 0 || decoded.length > MAX_ADMIN_IMAGE_BYTES) {
                throw new ApiException(AiErrorCode.AI_400_004);
            }
            // ★매직바이트 검증: 신고 mediaType 과 무관하게 실바이트가 JPEG/PNG/WEBP 가 아니면 거부.
            if (!isAllowedImageSignature(decoded)) {
                throw new ApiException(AiErrorCode.AI_400_004);
            }
            out.add(new ImagePart(img.getBase64(), img.getMediaType()));
        }
        return out;
    }

    /**
     * 실바이트 매직 시그니처 확인: JPEG(FF D8) / PNG(89 50 4E 47) / WEBP(RIFF….WEBP —
     * offset0~3 = 'RIFF', offset8~11 = 'WEBP') 중 하나만 허용.
     */
    private boolean isAllowedImageSignature(byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }
        // JPEG: FF D8
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            return true;
        }
        // PNG: 89 50 4E 47
        if ((data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
            return true;
        }
        // WEBP: 'RIFF' + 4바이트 크기 + 'WEBP'
        if (data.length >= 12
                && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
            return true;
        }
        return false;
    }

    /**
     * 도출 질의 = 구조화 값(공정명/위험성분류/유해요인명/유해요인설명) + 관리자 보완 설명 +
     * 이미지 확정 대화 전체(visible user+assistant, 역할 라벨 부착 — v3.7).
     */
    private String buildDeriveQuery(RiskAssessmentAiSource source, RiskAiDerivationRow before, String suppDesc) {
        StringBuilder sb = new StringBuilder();
        appendLabeled(sb, "공정명", source.processNm());
        appendLabeled(sb, "위험성 분류", source.riskTypeNm());
        appendLabeled(sb, "유해요인명", source.hazardNm());
        // ★AI 블록 스트립: 팝업 저장으로 덧붙은 이전 도출 결과가 재입력되는 피드백 루프 오염 차단
        appendLabeled(sb, "유해요인 설명", stripAiAppendix(source.initDesc()));
        appendLabeled(sb, "관리자 보완 설명", suppDesc);

        String dialogue = joinVisibleDialogue(before == null ? null : before.imgChatJson());
        appendLabeled(sb, "이미지 확정 대화", dialogue);
        return sb.toString().trim();
    }

    /**
     * 이미지 분석(VLM) 시스템 프롬프트 뒤에 근로자 입력(공정명/위험성분류/유해요인명/유해요인설명)과
     * 관리자 보완 설명을 참고 맥락으로 append. ★요청빌드 시점에만 주입하고 IMG_CHAT_JSON 엔 저장하지 않는다.
     */
    private String buildImageContext(RiskAssessmentAiSource source, String suppDesc) {
        StringBuilder ctx = new StringBuilder();
        appendLabeled(ctx, "공정명", source.processNm());
        appendLabeled(ctx, "위험성 분류", source.riskTypeNm());
        appendLabeled(ctx, "유해요인명", source.hazardNm());
        // ★AI 블록 스트립: 이전 도출 결과의 재입력(피드백 루프 오염) 차단 — 화면 표시는 원문 유지
        appendLabeled(ctx, "유해요인 설명", stripAiAppendix(source.initDesc()));
        appendLabeled(ctx, "관리자 보완 설명", suppDesc);

        if (ctx.length() == 0) {
            return IMG_CONFIRM_SYSTEM_PROMPT;
        }
        return IMG_CONFIRM_SYSTEM_PROMPT
            + "\n\n아래는 이 사진이 첨부된 위험성평가 건의 참고 정보다(사진 해석의 맥락으로만 활용):\n"
            + "[근로자 입력 정보]\n"
            + ctx.toString().trim();
    }

    /**
     * 유효 보완 설명 결정: 요청 값(param.suppDesc)이 있으면 그 값(클램프), 없으면 저장된 값(before.suppDesc).
     * 저장(updateSuppDesc)은 호출부에서 param.suppDesc 가 있을 때만 수행한다.
     */
    private String resolveSuppDesc(RiskAiParam param, RiskAiDerivationRow before) {
        if (StringUtils.hasText(param.suppDesc())) {
            return clampSupp(param.suppDesc());
        }
        if (before != null && StringUtils.hasText(before.suppDesc())) {
            return before.suppDesc().trim();
        }
        return "";
    }

    /** 보완 설명 저장 전 트림 + 길이 클램프(SUPP_DESC 컬럼 절단 방어). null 은 그대로(컬럼 NULL 허용). */
    private String clampSupp(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.length() > MAX_SUPP_DESC_LEN ? t.substring(0, MAX_SUPP_DESC_LEN) : t;
    }

    /**
     * INIT_DESC 에서 팝업 저장으로 덧붙은 AI 반영 블록([AI분석 유해요인] 마커부터 끝)을 잘라낸다.
     * WEB_003 저장 액션 이후, 이전 도출 결과가 다음 도출의 "근로자 설명" 입력으로 재유입되는
     * 피드백 루프 오염을 차단 — LLM 입력에만 적용하고 화면 표시(FE summary)는 원문 그대로 둔다.
     * 마커 미존재 시 원문 그대로, null 안전.
     */
    private String stripAiAppendix(String initDesc) {
        if (initDesc == null) {
            return null;
        }
        int pos = initDesc.indexOf(AI_APPENDIX_MARKER);
        if (pos < 0) {
            return initDesc;
        }
        return initDesc.substring(0, pos).trim();
    }

    /** "[라벨]값\n" 형식으로 append(값이 공백이면 생략). */
    private void appendLabeled(StringBuilder sb, String label, String value) {
        if (StringUtils.hasText(value)) {
            sb.append('[').append(label).append(']').append(value.trim()).append('\n');
        }
    }

    /**
     * IMG_CHAT_JSON 의 visible 대화 전체(user+assistant, hidden 제외)를 역할 라벨과 함께
     * 등장 순서대로 결합(v3.7 — 구 joinAssistantUtterances 대체).
     * ★사용자 확정: 자유 대화형 전환으로 관리자 채팅 발화가 곧 관리자 의견 역할이므로
     *   assistant 발화만이 아니라 양쪽을 도출 질의에 반영한다.
     */
    private String joinVisibleDialogue(String imgChatJson) {
        List<ChatTurn> turns = parseChatTurns(imgChatJson);
        StringBuilder sb = new StringBuilder();
        for (ChatTurn t : turns) {
            if (t == null || Boolean.TRUE.equals(t.hidden()) || !StringUtils.hasText(t.text())) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(ROLE_USER.equals(t.role()) ? "관리자: " : "AI: ").append(t.text().trim());
        }
        return sb.toString();
    }

    /**
     * recompose 청크로 그라운딩 컨텍스트 조립. marker([n])↔hit 매핑을 채운다.
     * ★LLM 출력 스키마에서 citations 를 제거했으므로 chunkId 는 컨텍스트에 넣지 않는다
     *   (서버가 markers→markerToHit 대조로 citations 를 결정적 조립 — 프롬프트 축소).
     * ★verbatim 은 인자로 받지 않으므로 원문이 이 문자열에 절대 포함되지 않는다(하드가드 #4).
     */
    private String buildGroundedContext(String query, List<RagHit> recompose,
                                        Map<Integer, RagHit> markerToHit) {
        StringBuilder sb = new StringBuilder();
        sb.append("[근거 청크]\n");
        int n = 1;
        for (RagHit h : recompose) {
            markerToHit.put(n, h);
            sb.append('[').append(n).append("] (출처: ").append(nz(h.getSourceName()))
              .append(" / 신뢰등급: ").append(nz(h.getDataReliability())).append(")\n");
            // ★그라운딩 개선 A: content(사고 개요/본문)는 항상 포함하고,
            //   hazardText/measureText 는 값이 있을 때만 라벨과 함께 병기(빈 필드는 라벨째 생략).
            //   (기존 로직은 hazard/measure 존재 시 content 를 버려, hazard_text 가 빈 청크에서
            //    LLM 근거가 "유해요인:(빈칸)" 수준으로 붕괴하는 결함이 있었다.)
            StringBuilder body = new StringBuilder();
            if (StringUtils.hasText(h.getContent())) {
                body.append("사고개요: ").append(h.getContent().trim()).append('\n');
            }
            if (StringUtils.hasText(h.getHazardText())) {
                body.append("유해요인: ").append(h.getHazardText().trim()).append('\n');
            }
            if (StringUtils.hasText(h.getMeasureText())) {
                body.append("감소대책: ").append(h.getMeasureText().trim()).append('\n');
            }
            sb.append(body).append('\n');
            n++;
        }
        sb.append("[위험성평가 요청] ").append(query);
        return sb.toString();
    }

    // ==================================================================
    // 내부 헬퍼 — verbatim 선택 전용 재랭킹(v3.9)
    // ==================================================================

    /**
     * verbatim(3유형) 참고 원문 선정: 전용 검색(track=verbatim, topK=10) → 선택 전용 LLM 재랭킹.
     *
     * <p>★하드가드 #4 개정(v3.9): 후보 원문을 선택 전용 호출의 입력으로는 보여주되 출력은 번호만
     *    허용(서버가 숫자 외 전부 폐기)하고, 화면 표시는 서버가 DB 원문을 그대로 렌더한다 —
     *    LLM 출력물에 3유형 텍스트가 포함되지 않으므로 원문 변형 배포가 구조적으로 불가.
     * <p>폴백: 선택 호출 실패(HTTP 오류/refusal/V: 줄 미발견)면 벡터 유사도 상위
     *    {@code MAX_VERBATIM_REFS}건 폴백(기능 후퇴 없음). "V: 없음"(명시적 무관련 판정)은
     *    빈 목록 채택(판정 존중 — 폴백 아님).
     * <p>어떤 예외도 derive 본류로 전파하지 않는다(try/catch 격리).
     */
    private List<VerbatimRefItem> selectVerbatimRefs(RiskAiParam param, String query) {
        // 후보 전용 검색: track 필터로 verbatim 만 조회(SEARCH_SQL 의 track ANY 필터 사용).
        //   ★중복 제거로 후보가 줄어들 것을 감안해 넓게 조회(Ai01Service maxTopK 클램프 내).
        List<RagHit> candidates;
        try {
            // prafta-062 D5: 법령 조문(data_reliability='법령')은 참고 원문(SIF 사례) 후보에서 배제 —
            //   법령은 전용 트랙(searchLawTrack)에서만 다룬다(도출 "참고 원문" 오염 방지 = 무회귀).
            RagSearchParam searchParam = new RagSearchParam(
                query, VERBATIM_CANDIDATE_TOP_K, null, null, List.of(VERBATIM_TRACK),
                List.of(LAW_RELIABILITY), param.gvUserCd(), param.gvCmpnyCd());
            RagSearchResponse resp = ai01Service.search(searchParam);
            candidates = (resp.getHits() == null) ? List.of() : resp.getHits();
        } catch (Exception e) {
            log.warn("verbatim 전용 검색 실패 - 참고 원문 없이 진행. 원인={}", e.getMessage());
            return List.of();
        }
        // ★후보 중복 제거: SIF 는 같은 유해요인·감소대책이 사례 행마다 반복되는 구조 —
        //   유해요인+감소대책이 같은 행은 유사도 상위 1건만 남겨 선택 LLM 에 다양한 후보를 준다.
        candidates = dedupVerbatimCandidates(candidates);
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<RagHit> chosen;
        try {
            String context = buildVerbatimSelectContext(query, candidates);
            LlmRawResponse raw = llmAnswerClient.answer(VERBATIM_SELECT_SYSTEM_PROMPT, context);
            // ★쿼터 사용량 누적 — 선택 전용 호출도 실호출(과금)이므로 raw 수신 즉시 기록.
            aiQuotaService.record(param.gvCmpnyCd(), raw.inputTokens(), raw.outputTokens());
            List<Integer> picked = raw.refusal() ? null : parseVerbatimSelection(raw.combinedText(), candidates.size());

            // 감사: 선택 호출도 실호출 1:1 기록(선정 청크 ID 누적, 선정 0건이면 abstained=true 성격).
            List<String> usedIds = new ArrayList<>();
            if (picked != null) {
                for (Integer n : picked) {
                    usedIds.add(candidates.get(n - 1).getChunkId());
                }
            }
            recordCall(param, ENDPOINT_DERIVE_SELECT, raw, query.length(), usedIds,
                picked == null || picked.isEmpty());

            if (picked == null) {
                // 오류성(refusal / V: 줄 미발견) → 벡터 상위 건 폴백.
                log.warn("verbatim 선택 호출 실패(refusal/파싱 불가) - 벡터 상위 {}건 폴백", MAX_VERBATIM_REFS);
                chosen = candidates.subList(0, Math.min(MAX_VERBATIM_REFS, candidates.size()));
            } else if (picked.isEmpty()) {
                // 명시적 무관련 판정("V: 없음") — 판정 존중(폴백 아님).
                log.info("verbatim 선택: 후보 {}건 중 관련 항목 없음 판정 - 빈 목록 채택", candidates.size());
                chosen = List.of();
            } else {
                chosen = new ArrayList<>();
                for (Integer n : picked) {
                    chosen.add(candidates.get(n - 1));
                }
            }
        } catch (Exception e) {
            // 선택 호출 예외가 derive 전체를 죽이면 안 됨 — 벡터 상위 건 폴백으로 격리.
            log.warn("verbatim 선택 호출 예외 - 벡터 상위 {}건 폴백. 원인={}", MAX_VERBATIM_REFS, e.getMessage());
            chosen = candidates.subList(0, Math.min(MAX_VERBATIM_REFS, candidates.size()));
        }

        // 원문 무변경 패스스루 수집(LLM 미경유 — 서버가 검색 결과에서 직접 전달, chunkId 는 내부 로깅만).
        // ★동일 유해요인명 병합: 같은 유해요인명(공백 정규화 비교)의 선정 행은 1개 항목으로 묶고,
        //   감소대책은 줄 단위로 모아 완전 동일한 줄만 제거한다(줄 텍스트 자체는 원문 그대로 —
        //   배열·중복 제거일 뿐 변경이 아님). 재해개요/출처 필드는 첫 행 기준(동일 출처 자료).
        Map<String, Integer> hazardKeyToIdx = new LinkedHashMap<>();
        List<RagHit> firstRows = new ArrayList<>();
        List<LinkedHashSet<String>> mergedMeasureLines = new ArrayList<>();
        for (RagHit h : chosen) {
            log.debug("verbatim 참고 원문 채택 - chunkId={}, source={}", h.getChunkId(), h.getSourceName());
            String key = normSpace(h.getHazardText());
            Integer idx = key.isEmpty() ? null : hazardKeyToIdx.get(key);
            if (idx == null) {
                idx = firstRows.size();
                if (!key.isEmpty()) {
                    hazardKeyToIdx.put(key, idx);
                }
                firstRows.add(h);
                mergedMeasureLines.add(new LinkedHashSet<>());
            }
            if (StringUtils.hasText(h.getMeasureText())) {
                for (String line : h.getMeasureText().split("\\R")) {
                    String t = line.trim();
                    if (!t.isEmpty()) {
                        mergedMeasureLines.get(idx).add(t);
                    }
                }
            }
        }
        List<VerbatimRefItem> out = new ArrayList<>();
        for (int i = 0; i < firstRows.size(); i++) {
            RagHit h = firstRows.get(i);
            String mergedMeasure = mergedMeasureLines.get(i).isEmpty()
                ? h.getMeasureText()
                : String.join("\n", mergedMeasureLines.get(i));
            // prafta-062: 근거 층위(출처 단위) — 코드+표시 문구는 서버 대조값으로 채운다(LLM 값 미사용).
            out.add(new VerbatimRefItem(
                h.getSourceName(), h.getDataReliability(),
                h.getContent(), h.getHazardText(), mergedMeasure,
                h.getSourceOrg(), h.getSourceUrl(), h.getLicenseType(),
                h.getEvidenceTier(), EvidenceTierLabels.labelOf(h.getEvidenceTier())));
        }
        return out;
    }

    // ==================================================================
    // 내부 헬퍼 — 법령 전용 검색 트랙(prafta-062 배포 C)
    // ==================================================================

    /**
     * 법령 전용 검색(prafta-062): track=verbatim + data_reliability='법령' 청크만 topK={@code LAW_SEARCH_TOP_K}
     * 조회하고, {@code lawMinScore}(기본 0.40 = deriveMinScore 와 동일 출발) 미만은 채택하지 않는다.
     *
     * <p>법령은 소수 트랙(전체의 약 2.7%)이라 HNSW 필터검색 기아가 우려되므로 전용 쿼리로 분리하고,
     *    관측 로그로 후보 수·최상위 점수를 남겨 기아 여부(후보 0건)를 즉시 식별한다(요청서 S3-2).
     *    임계값은 knob 만 신설(D4=(b)) — 적재 후 점수 분포 관측 뒤 조정한다.
     * <p>어떤 예외도 호출부(도출 본류)로 전파하지 않는다(try/catch 격리 — selectVerbatimRefs 패턴 미러.
     *    단 법령은 유사도 미달 조문을 억지로 붙이면 오인용이라 폴백 없이 빈 목록이 맞다).
     *
     * <p>호출부: 배포 D(062-07) {@link #attachLawRefs} — 유해요인별 사후 매핑 전용.
     */
    private List<RagHit> searchLawTrack(RiskAiParam param, String query) {
        List<RagHit> hits;
        try {
            RagSearchParam searchParam = new RagSearchParam(
                query, LAW_SEARCH_TOP_K, null, List.of(LAW_RELIABILITY), List.of(VERBATIM_TRACK),
                null, param.gvUserCd(), param.gvCmpnyCd());
            RagSearchResponse resp = ai01Service.search(searchParam);
            hits = (resp.getHits() == null) ? List.of() : resp.getHits();
        } catch (Exception e) {
            // 법령 검색 실패가 도출 본류를 죽이면 안 됨 — 빈 목록으로 격리(법령은 폴백 없음이 정책).
            log.warn("법령 트랙 검색 실패 - 법령 첨부 없이 진행. 원인={}", e.getMessage());
            return List.of();
        }

        // 임계값 판정: lawMinScore 미만 조문은 채택하지 않는다(무관 조문 억지 첨부 방지).
        double minScore = aiProperties.getSearch().getLawMinScore();
        double topScore = hits.isEmpty() ? Double.NaN : hits.get(0).getScore();
        List<RagHit> adopted = new ArrayList<>();
        for (RagHit h : hits) {
            if (h.getScore() >= minScore) {
                adopted.add(h);
            }
        }
        // ★관측 로그(임계값 튜닝·기아 감지 근거): 511~515행 "RAG 그라운딩 판정" 형식 미러.
        log.info("법령 트랙 판정 - 질의길이={}, 후보={}, 최상위점수={}, 임계값={}, 채택={}건",
                query.length(), hits.size(),
                Double.isNaN(topScore) ? "N/A" : String.format("%.4f", topScore),
                String.format("%.2f", minScore), adopted.size());
        return adopted;
    }

    /**
     * 유해요인별 법령 사후 매핑(prafta-062 배포 D — 062-07).
     *
     * <p>확정된 유해요인 목록(상위 {@code MAX_LAW_MAP_HAZARDS}건)마다 법령 트랙을 검색해
     *    매칭 조문(유해요인당 최대 {@code MAX_LAW_REFS_PER_HAZARD}건)을 원문 그대로 첨부한다.
     *    검색 키 = 짧은 컨텍스트(공정명/위험성 분류) + 유해요인 텍스트 — 조문은 추상적이라
     *    유해요인 한 줄만으로는 유사도가 낮게 나올 수 있어 컨텍스트를 보강한다(질의 클램프는 기존 재사용).
     * <p>★불변식(핵심 회귀 축): 유해요인의 추가/삭제/재정렬 없이 lawRefs 첨부만 한다 —
     *    반환 목록의 크기·순서·text/markers/measures 는 입력과 동일하다.
     * <p>매칭 없으면 lawRefs=null 유지(부정 문구·폴백 금지 — 원칙 4). 검색 격리는 searchLawTrack 내부
     *    try/catch 가 담당하고, 여기서는 조립 예외만 추가 격리한다(실패해도 도출 본류 무영향).
     */
    private List<DerivedHazardGroup> attachLawRefs(RiskAiParam param, RiskAssessmentAiSource source,
                                                   List<DerivedHazardGroup> hazards) {
        if (hazards == null || hazards.isEmpty()) {
            return hazards;
        }
        long startMs = System.currentTimeMillis();
        List<DerivedHazardGroup> out = new ArrayList<>(hazards);
        int mappedCnt = 0;
        try {
            // 짧은 컨텍스트: 공정명/위험성 분류(buildDeriveQuery 라벨 관례 미러 — 있는 값만).
            StringBuilder ctx = new StringBuilder();
            if (source != null) {
                if (StringUtils.hasText(source.processNm())) {
                    ctx.append("공정명: ").append(source.processNm()).append('\n');
                }
                if (StringUtils.hasText(source.riskTypeNm())) {
                    ctx.append("위험성 분류: ").append(source.riskTypeNm()).append('\n');
                }
            }
            int limit = Math.min(MAX_LAW_MAP_HAZARDS, out.size());
            for (int i = 0; i < limit; i++) {
                DerivedHazardGroup hz = out.get(i);
                if (hz == null || !StringUtils.hasText(hz.text())) {
                    continue;
                }
                // 과대입력 방어: 검색·임베딩 투입 전 길이 클램프(도출 질의와 동일 상한 재사용).
                String lawQuery = ctx + "유해요인: " + hz.text();
                if (lawQuery.length() > MAX_DERIVE_QUERY_LEN) {
                    lawQuery = lawQuery.substring(0, MAX_DERIVE_QUERY_LEN);
                }
                List<RagHit> adopted = searchLawTrack(param, lawQuery);
                if (adopted.isEmpty()) {
                    continue; // 매칭 없음 = 미첨부(lawRefs=null 유지 — 억지 첨부·부정 문구 금지)
                }
                List<RagHit> top = adopted.subList(0, Math.min(MAX_LAW_REFS_PER_HAZARD, adopted.size()));
                List<LawRefItem> refs = new ArrayList<>();
                for (RagHit h : top) {
                    refs.add(toLawRefItem(h));
                }
                // ★첨부만: text/markers/measures 원본 그대로 승계(개수·순서 불변).
                out.set(i, new DerivedHazardGroup(hz.text(), hz.markers(), hz.measures(), List.copyOf(refs)));
                mappedCnt++;
            }
        } catch (Exception e) {
            // 조립 예외 격리 — 이미 첨부된 항목은 유지, 나머지는 미첨부로 진행(도출 본류 무영향).
            log.warn("법령 사후 매핑 조립 실패 - 일부/전체 미첨부로 진행. 원인={}", e.getMessage());
        }
        // ★지연 계측(배포 후 판단 근거): TEI 단건 왕복 × 최대 MAX_LAW_MAP_HAZARDS 회가 추가된다.
        //   합계가 FE derive 타임아웃(90초) 대비 위험하면 MAX_LAW_MAP_HAZARDS 를 낮춘다.
        log.info("법령 매핑 소요 - 유해요인={}건(매핑 {}건), 합계={}ms",
            hazards.size(), mappedCnt, System.currentTimeMillis() - startMs);
        return out;
    }

    /**
     * RagHit(법령 청크) → LawRefItem. 원문({@code content})은 무변경 패스스루, 표시 문구
     * (articleLabel/effectiveDateText/evidenceTierLabel)는 서버가 완성한다(FE 무로직 원칙).
     */
    private LawRefItem toLawRefItem(RagHit h) {
        return new LawRefItem(
            h.getSourceName(),                                   // 법령 정식명칭(registry source_name)
            buildArticleLabel(h.getSourceLocator(), h.getArticleTitle()),
            h.getContent(),                                      // 조문 원문 그대로(가공·절단 금지)
            h.getSourceLocator(),
            h.getArticleEffectiveDate(),
            formatEffectiveDate(h.getArticleEffectiveDate()),
            h.getSourceUrl(),
            h.getEvidenceTier(),
            EvidenceTierLabels.labelOf(h.getEvidenceTier()));
    }

    /**
     * 조문 라벨 조립 — 예: "제32조의2(보호구의 지급 등)".
     *
     * <p>조립 방식: sourceLocator("{법령약칭} 제{조문번호}조[의{가지번호}]" — 062-05 §4 규격)에서
     *    첫 공백 뒤의 "제…조…" 부분을 취하고, 조문제목(meta_json->>'article_title' — SEARCH_SQL additive
     *    확장으로 RagHit 에 실림)이 있으면 괄호로 덧붙인다. 형식 이탈 시 로케이터 원문 폴백(표기 누락 방지).
     */
    private String buildArticleLabel(String sourceLocator, String articleTitle) {
        String article = null;
        if (StringUtils.hasText(sourceLocator)) {
            int idx = sourceLocator.indexOf(" 제");
            article = (idx >= 0) ? sourceLocator.substring(idx + 1).trim() : sourceLocator.trim();
        }
        if (!StringUtils.hasText(article)) {
            return StringUtils.hasText(articleTitle) ? "(" + articleTitle.trim() + ")" : null;
        }
        if (StringUtils.hasText(articleTitle)) {
            return article + "(" + articleTitle.trim() + ")";
        }
        return article;
    }

    /** 조문시행일자(YYYYMMDD) → 표시용 "YYYY.MM.DD". 8자리 숫자가 아니면 null(FE v-if 미표시). */
    private String formatEffectiveDate(String yyyymmdd) {
        if (yyyymmdd == null || !yyyymmdd.matches("\\d{8}")) {
            return null;
        }
        return yyyymmdd.substring(0, 4) + "." + yyyymmdd.substring(4, 6) + "." + yyyymmdd.substring(6, 8);
    }

    /**
     * verbatim 후보 중복 제거: 유해요인+감소대책의 공백 정규화 문자열이 같은 행은
     * 유사도 상위 1건만 남긴다(비교 전용 정규화 — 표시 텍스트는 원문 유지).
     */
    private List<RagHit> dedupVerbatimCandidates(List<RagHit> candidates) {
        List<RagHit> out = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (RagHit h : candidates) {
            String key = normSpace(h.getHazardText()) + "|" + normSpace(h.getMeasureText());
            if (seen.add(key)) {
                out.add(h);
            }
        }
        return out;
    }

    /** 공백 정규화(중복 비교 전용 — 표시 텍스트는 원문 유지). */
    private String normSpace(String s) {
        return (s == null) ? "" : s.replaceAll("\\s+", " ").trim();
    }

    /** 선택 전용 컨텍스트: 후보 원문을 [1]..[N] 번호와 함께 나열(있는 필드만) + 요청. */
    private String buildVerbatimSelectContext(String query, List<RagHit> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("[후보 원문]\n");
        int n = 1;
        for (RagHit h : candidates) {
            sb.append('[').append(n).append("]\n");
            if (StringUtils.hasText(h.getHazardText())) {
                sb.append("유해요인: ").append(h.getHazardText().trim()).append('\n');
            }
            if (StringUtils.hasText(h.getMeasureText())) {
                sb.append("감소대책: ").append(h.getMeasureText().trim()).append('\n');
            }
            if (StringUtils.hasText(h.getContent())) {
                sb.append("재해개요: ").append(h.getContent().trim()).append('\n');
            }
            sb.append('\n');
            n++;
        }
        sb.append("[위험성평가 요청] ").append(query);
        return sb.toString();
    }

    /**
     * 선택 응답 파싱: "V:" 줄에서 숫자만 추출(NUMBER_PATTERN — 숫자 외 텍스트는 전부 무시,
     * 원문 유출 차단). 후보 범위 밖 번호·중복은 폐기, 최대 {@code MAX_VERBATIM_REFS}건.
     *
     * @return 선정 번호(1-base) 목록. "V: 없음" 등 숫자 0개인 V: 줄 = 빈 리스트(명시적 무관련 판정).
     *         V: 줄 자체가 없으면 null(오류성 — 호출부 폴백 대상).
     */
    private List<Integer> parseVerbatimSelection(String combinedText, int candidateCount) {
        if (combinedText == null || combinedText.isBlank()) {
            return null;
        }
        for (String rawLine : combinedText.split("\\R")) {
            String line = stripBullet(rawLine.trim());
            if (!line.regionMatches(true, 0, "V:", 0, 2)) {
                continue;   // V: 외 줄은 무시(라인 프로토콜 파서와 동일한 관용성)
            }
            List<Integer> out = new ArrayList<>();
            Matcher m = NUMBER_PATTERN.matcher(line.substring(2));
            while (m.find() && out.size() < MAX_VERBATIM_REFS) {
                int n = Integer.parseInt(m.group());
                if (n >= 1 && n <= candidateCount && !out.contains(n)) {
                    out.add(n);
                }
            }
            return out;   // 첫 V: 줄만 채택(숫자 0개 = "없음" 판정)
        }
        return null;
    }

    // ==================================================================
    // 내부 헬퍼 — 파싱/검증
    // ==================================================================

    /**
     * LLM 라인 프로토콜 응답 파싱(v3.6).
     *
     * <p>★전환 사유: HCX-005 가 중첩 JSON 의 괄호 폐합을 상습적으로 못 맞춰(hazards 배열 미폐합 등
     *    문법 오류 연발 — v3.5 방어 파싱도 readTree 단계에서 무력) JSON 을 전면 폐기하고
     *    줄 단위 프로토콜(ABSTAIN / H: / M:)로 전환했다.
     * <p>파싱 규칙(줄 단위 순회 — trim + 선행 불릿 "-","•","*","▶" 제거 후):
     *   <ul>
     *     <li>"ABSTAIN" 시작(대소문자 무관) → 값이 Y/true 면 abstained.</li>
     *     <li>"H:" 시작 → 새 유해요인(마커 [n] 전량 수집 후 본문에서 제거).</li>
     *     <li>"M:" 시작 → 현재 유해요인의 개선안(선행 H: 없으면 warn 후 skip).</li>
     *     <li>그 외 줄은 무시(LLM 사족 허용 — warn 불요).</li>
     *   </ul>
     * <p>markers 는 "[n]" 문자열 형식으로 담아 후속 로직(buildCitationsFromMarkers/parseMarkerNumber/
     *    filterHazardMarkers/stripHazardMarkers)에 무변경 연결된다.
     * <p>유효 hazard(text 비공백) 0건 + ABSTAIN 표기도 없으면 null 반환 — 호출부의
     *    재시도→자유생성 폴백 흐름(v3.5) 그대로 유지.
     */
    private DeriveLlmResult parseDerive(String combinedText) {
        if (combinedText == null || combinedText.isBlank()) {
            log.error("LLM 응답이 비어 있음");
            return null;
        }

        boolean abstainSeen = false;
        boolean abstained = false;
        List<DeriveLlmResult.HazardOut> hazards = new ArrayList<>();
        // 진행 중 유해요인(H: 줄 이후 M: 줄 수집 대상). 다음 H: 또는 순회 종료 시 확정 append.
        String curText = null;
        List<String> curMarkers = null;
        List<DeriveLlmResult.Item> curMeasures = null;

        for (String rawLine : combinedText.split("\\R")) {
            String line = stripBullet(rawLine.trim());
            if (line.isEmpty()) {
                continue;
            }
            if (line.regionMatches(true, 0, "ABSTAIN", 0, 7)) {
                abstainSeen = true;
                String value = line.substring(7).replace(":", " ").trim();
                abstained = value.regionMatches(true, 0, "Y", 0, 1)
                         || value.regionMatches(true, 0, "TRUE", 0, 4);
                continue;
            }
            if (line.regionMatches(true, 0, "H:", 0, 2)) {
                appendHazard(hazards, curText, curMarkers, curMeasures);   // 직전 유해요인 확정
                List<String> markers = new ArrayList<>();
                curText = extractMarkers(line.substring(2), markers);
                curMarkers = markers;
                curMeasures = new ArrayList<>();
                continue;
            }
            if (line.regionMatches(true, 0, "M:", 0, 2)) {
                if (curText == null) {
                    log.warn("선행 H: 없는 M: 줄 skip - line={}", line);
                    continue;
                }
                List<String> markers = new ArrayList<>();
                String text = extractMarkers(line.substring(2), markers);
                if (!text.isEmpty()) {
                    curMeasures.add(new DeriveLlmResult.Item(text, markers));
                }
                continue;
            }
            // 그 외 줄(머리말/사족 등)은 무시.
        }
        appendHazard(hazards, curText, curMarkers, curMeasures);   // 마지막 유해요인 확정

        if (hazards.isEmpty() && !abstainSeen) {
            log.error("LLM 라인 프로토콜 응답에서 H:/ABSTAIN 줄을 찾지 못함");
            return null;
        }
        return new DeriveLlmResult(abstained, hazards);
    }

    /** 진행 중 유해요인 확정 append(text null=미시작, 공백=warn 후 skip). */
    private void appendHazard(List<DeriveLlmResult.HazardOut> hazards, String text,
                              List<String> markers, List<DeriveLlmResult.Item> measures) {
        if (text == null) {
            return;
        }
        if (text.isBlank()) {
            log.warn("H: 본문 공백 요소 skip");
            return;
        }
        hazards.add(new DeriveLlmResult.HazardOut(text, markers, measures));
    }

    /** 본문에서 "[n]" 마커를 전량 수집(문자열 "[n]" 형식 유지)하고, 마커를 제거한 본문 trim 을 반환. */
    private String extractMarkers(String body, List<String> markers) {
        Matcher m = MARKER_PATTERN.matcher(body);
        while (m.find()) {
            markers.add("[" + m.group(1) + "]");
        }
        return m.replaceAll("").trim();
    }

    /** 선행 불릿 기호("-", "•", "*", "▶") 제거 + trim(LLM 이 목록 기호를 붙이는 경우 방어). */
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

    /**
     * LLM 이 반환한 hazards/measures 의 markers 를 전수 수집하고, [근거 청크]에 실재하는
     * ([n]→markerToHit) 마커만 유효 채택해 citations(서버 메타: marker/sourceName/dataReliability)와
     * usedChunkIds(감사, 중복 제거)를 결정적으로 조립한다.
     *
     * <p>★LLM 출력 스키마에서 citations 를 제거한 후속(HCX JSON 안정화) — 구 validateCitations 대체.
     *    markerToHit 에 없는 마커(환각 마커)는 무효 폐기 — 호출부의 filterHazardMarkers 로 항목의
     *    markers 에서도 함께 제거된다(환각 인용 차단 유지).
     */
    private List<CitationItem> buildCitationsFromMarkers(List<DeriveLlmResult.HazardOut> items,
                                                         Map<Integer, RagHit> markerToHit,
                                                         Set<String> validMarkers, List<String> usedChunkIds) {
        // 마커 전수 수집(등장 순서 유지 + 중복 제거).
        LinkedHashSet<String> markers = new LinkedHashSet<>();
        if (items != null) {
            for (DeriveLlmResult.HazardOut hz : items) {
                if (hz == null) {
                    continue;
                }
                if (hz.markers() != null) {
                    markers.addAll(hz.markers());
                }
                if (hz.measures() != null) {
                    for (DeriveLlmResult.Item ms : hz.measures()) {
                        if (ms != null && ms.markers() != null) {
                            markers.addAll(ms.markers());
                        }
                    }
                }
            }
        }
        List<CitationItem> out = new ArrayList<>();
        LinkedHashSet<String> seenChunkIds = new LinkedHashSet<>();
        for (String m : markers) {
            Integer n = parseMarkerNumber(m);
            RagHit hit = (n == null) ? null : markerToHit.get(n);
            if (hit == null) {
                log.warn("근거 청크에 없는 마커 폐기 - marker={}", m);
                continue;
            }
            validMarkers.add(m);
            // prafta-062: 근거 층위(출처 단위) — 코드+표시 문구는 서버 대조값으로 채운다(LLM 값 미사용).
            out.add(new CitationItem(m, hit.getSourceName(), hit.getDataReliability(),
                hit.getSourceOrg(), hit.getSourceUrl(),
                hit.getEvidenceTier(), EvidenceTierLabels.labelOf(hit.getEvidenceTier())));
            if (seenChunkIds.add(hit.getChunkId())) {
                usedChunkIds.add(hit.getChunkId());
            }
        }
        return out;
    }

    /** "[n]" 형식 마커에서 청크 번호 추출. 형식 위반이면 null(무효 마커). */
    private Integer parseMarkerNumber(String marker) {
        if (marker == null || marker.length() < 3
                || marker.charAt(0) != '[' || marker.charAt(marker.length() - 1) != ']') {
            return null;
        }
        try {
            return Integer.valueOf(marker.substring(1, marker.length() - 1).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 자유생성 그룹: 유해요인·중첩 개선안 양쪽 markers 를 빈 배열로 강제(각주 없음)(v3). */
    private List<DerivedHazardGroup> stripHazardMarkers(List<DeriveLlmResult.HazardOut> items) {
        List<DerivedHazardGroup> out = new ArrayList<>();
        if (items == null) {
            return out;
        }
        for (DeriveLlmResult.HazardOut hz : items) {
            if (hz == null || !StringUtils.hasText(hz.text())) {
                continue;
            }
            List<DerivedItem> measures = new ArrayList<>();
            if (hz.measures() != null) {
                for (DeriveLlmResult.Item ms : hz.measures()) {
                    if (ms == null || !StringUtils.hasText(ms.text())) {
                        continue;
                    }
                    measures.add(new DerivedItem(ms.text().trim(), List.of()));
                }
            }
            // lawRefs=null: 법령 첨부는 도출 확정 후 attachLawRefs 가 별도 수행(prafta-062 배포 D).
            out.add(new DerivedHazardGroup(hz.text().trim(), List.of(), measures, null));
        }
        return out;
    }

    /** 그라운딩 그룹: 유해요인·중첩 개선안 양쪽 markers 를 검증 통과 마커로만 필터(대롱거리는 각주 제거)(v3). */
    private List<DerivedHazardGroup> filterHazardMarkers(List<DeriveLlmResult.HazardOut> items,
                                                         Set<String> validMarkers) {
        List<DerivedHazardGroup> out = new ArrayList<>();
        if (items == null) {
            return out;
        }
        for (DeriveLlmResult.HazardOut hz : items) {
            if (hz == null || !StringUtils.hasText(hz.text())) {
                continue;
            }
            List<DerivedItem> measures = new ArrayList<>();
            if (hz.measures() != null) {
                for (DeriveLlmResult.Item ms : hz.measures()) {
                    if (ms == null || !StringUtils.hasText(ms.text())) {
                        continue;
                    }
                    measures.add(new DerivedItem(ms.text().trim(), filterValidMarkers(ms.markers(), validMarkers)));
                }
            }
            // lawRefs=null: 법령 첨부는 도출 확정 후 attachLawRefs 가 별도 수행(prafta-062 배포 D).
            out.add(new DerivedHazardGroup(hz.text().trim(), filterValidMarkers(hz.markers(), validMarkers), measures, null));
        }
        return out;
    }

    /** markers 중 검증 통과 마커만 잔존. */
    private List<String> filterValidMarkers(List<String> markers, Set<String> validMarkers) {
        List<String> out = new ArrayList<>();
        if (markers == null) {
            return out;
        }
        for (String m : markers) {
            if (m != null && validMarkers.contains(m)) {
                out.add(m);
            }
        }
        return out;
    }

    // ==================================================================
    // 내부 헬퍼 — 응답 조립
    // ==================================================================

    /** 저장 행 → 응답 DTO. 행이 없으면 초기 상태(대화/결과 없음). */
    private RiskAiDerivationResponse buildResponse(RiskAiDerivationRow row) {
        if (row == null) {
            return RiskAiDerivationResponse.builder()
                .chatTurns(List.of())
                .suppDesc("")
                .imgConfirmed(false)
                .hazards(List.of())
                .citations(List.of())
                .verbatimRefs(List.of())
                .abstained(false)
                .disclaimer(DISCLAIMER)
                .build();
        }
        return RiskAiDerivationResponse.builder()
            .chatTurns(parseChatTurns(row.imgChatJson()))
            .suppDesc(row.suppDesc() == null ? "" : row.suppDesc())
            .imgConfirmed("Y".equals(row.imgConfirmed()))
            .hazards(parseHazardGroups(row.hazardJson()))
            .citations(parseCitationList(row.citationJson()))
            .verbatimRefs(parseVerbatimList(row.verbatimJson()))
            .abstained("Y".equals(row.abstained()))
            .disclaimer(DISCLAIMER)
            .build();
    }

    private List<ChatTurn> parseChatTurns(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<ChatTurn> v = objectMapper.readValue(json, new TypeReference<List<ChatTurn>>() {});
            return (v == null) ? new ArrayList<>() : v;
        } catch (Exception e) {
            log.warn("대화 이력 JSON 파싱 실패(빈 배열 대체) - 원인={}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * HAZARD_JSON → 유해요인 그룹 목록(v3). 구버전 저장분([{text,markers}])은 measures=null 로 파싱되므로
     * 빈 리스트로 정규화한다(하위호환 read — FE 는 "다시 도출해 주세요" 안내).
     */
    private List<DerivedHazardGroup> parseHazardGroups(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<DerivedHazardGroup> v = objectMapper.readValue(json, new TypeReference<List<DerivedHazardGroup>>() {});
            if (v == null) {
                return List.of();
            }
            List<DerivedHazardGroup> out = new ArrayList<>();
            for (DerivedHazardGroup g : v) {
                if (g == null) {
                    continue;
                }
                // prafta-062: lawRefs 는 저장값 그대로 승계(재열람 복원). 구 저장분은 null → FE v-if 미표시.
                out.add(new DerivedHazardGroup(
                    g.text(),
                    g.markers() == null ? List.of() : g.markers(),
                    g.measures() == null ? List.of() : g.measures(),
                    g.lawRefs()));
            }
            return out;
        } catch (Exception e) {
            log.warn("도출 유해요인 그룹 JSON 파싱 실패(빈 배열 대체) - 원인={}", e.getMessage());
            return List.of();
        }
    }

    private List<CitationItem> parseCitationList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<CitationItem> v = objectMapper.readValue(json, new TypeReference<List<CitationItem>>() {});
            return (v == null) ? List.of() : v;
        } catch (Exception e) {
            log.warn("근거 출처 JSON 파싱 실패(빈 배열 대체) - 원인={}", e.getMessage());
            return List.of();
        }
    }

    /** VERBATIM_JSON → verbatim 참고 원문 목록(개선 C — 재열람 복원). 파싱 실패 시 빈 배열. */
    private List<VerbatimRefItem> parseVerbatimList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<VerbatimRefItem> v = objectMapper.readValue(json, new TypeReference<List<VerbatimRefItem>>() {});
            return (v == null) ? List.of() : v;
        } catch (Exception e) {
            log.warn("verbatim 참고 원문 JSON 파싱 실패(빈 배열 대체) - 원인={}", e.getMessage());
            return List.of();
        }
    }

    /** 객체 → JSON 문자열. 직렬화 실패 시 빈 배열(저장 무결성보다 응답 성공 우선). */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            log.warn("JSON 직렬화 실패 - 빈 배열로 대체", e);
            return "[]";
        }
    }

    // ==================================================================
    // 내부 헬퍼 — 감사·과금(best-effort)
    // ==================================================================

    /** 감사 로그 기록. raw==null 이면 토큰/비용 0. 실패해도 응답은 성공(경고 로그). */
    private void recordCall(RiskAiParam param, String endpoint, LlmRawResponse raw,
                            int queryLen, List<String> usedChunkIds, boolean abstained) {
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
                endpoint,
                model,
                queryLen,               // ★원문 미저장(길이만)
                input, output, cacheRead, cacheCreation,
                cost,
                usedChunkIds,
                abstained);
            aiCallRepository.save(logRow);
        } catch (Exception e) {
            log.warn("tb_ai_call 감사 로깅 실패(응답은 정상 반환) - 원인={}", e.getMessage());
        }
    }

    /**
     * HCX 과금 계산: 설정 단가(1k 토큰당, 기본 0) 기반. 캐시 단가 없음(cacheRead/cacheCreation 는 항상 0 → 무시).
     * (AnswerServiceImpl.computeCost 와 동일 정책 — 과금 산식 일원화 유지.)
     */
    private BigDecimal computeCost(String model, long input, long output, long cacheRead, long cacheCreation) {
        double inRate = aiProperties.getLlm().getCostPer1kInput();
        double outRate = aiProperties.getLlm().getCostPer1kOutput();
        double cost = (input / 1_000d) * inRate
                    + (output / 1_000d) * outRate;
        return new BigDecimal(cost, MathContext.DECIMAL64).setScale(5, RoundingMode.HALF_UP);
    }

    private String nz(String v) {
        return (v == null) ? "" : v;
    }

    private String safeTrim(String v) {
        return (v == null) ? "" : v.trim();
    }
}
