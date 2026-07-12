package com.prafta.web.tbm.tbmai01.service;

import com.prafta.web.tbm.tbmai01.application.param.TbmAiParam;
import com.prafta.web.tbm.tbmai01.application.param.TbmAiWorklistParam;
import com.prafta.web.tbm.tbmai01.dto.response.TbmAiAnalysisStatusResponse;
import com.prafta.web.tbm.tbmai01.dto.response.TbmAiItemResponse;
import com.prafta.web.tbm.tbmai01.dto.response.TbmAiWorklistResponse;

/**
 * TBM 세부항목 AI 분석·확정 서비스.
 *
 * <p>이미지(01)·PDF(04) 항목을 VLM 으로 분석해 초안(DRAFT)을 만들고, 대화형 보완(chat-item)과
 *    확정(confirm-item/manual-confirm)을 거쳐 CONFIRMED 로 전이한다. 상태·초안·대화·확정서술은
 *    {@code tb_tbm_edu_mtrl_item} 신규 컬럼에 직접 저장한다(별도 도출 테이블 없음).
 */
public interface TbmAi01Service {

    /** POST /analyze-items {mtrlCd} — 초기 큐잉(대상 항목 ANALYZING + 비동기 분석 트리거). */
    TbmAiAnalysisStatusResponse analyzeItems(TbmAiParam param);

    /**
     * 교육자료 저장 직후 자동 큐잉(best-effort). 게이트 OFF·대상 0건·예외는 삼키고 로그만 남긴다.
     *
     * <p>저장 흐름 보호를 위해 절대 예외를 밖으로 던지지 않는다. 반드시 트랜잭션 커밋 이후에 호출한다
     *    (비동기 러너가 별도 스레드에서 항목/AI_ANALYZE_YN 을 다시 읽으므로 커밋 전 트리거 시 미반영 위험).
     *
     * <p>사업장 인가는 이 메서드가 자체 수행한다(저장 경로는 회사 스코프까지만 검증하므로 위임 불가).
     *    권한이 없으면 예외 없이 큐잉만 스킵한다.
     *
     * @param mtrlCd  교육자료 코드(신규 저장이면 서비스가 채번한 값)
     * @param cmpnyCd 회사 스코프(JWT 도출)
     * @param userCd  감사 사용자(JWT 도출)
     * @param authCd  역할 코드(JWT 도출) — 회사공통/전사권한 판정에 사용
     */
    void enqueueOnSave(String mtrlCd, String cmpnyCd, String userCd, String authCd);

    /** GET /analysis-status {mtrlCd} — 자료 단위 항목별 AI 상태 조회(폴링). */
    TbmAiAnalysisStatusResponse analysisStatus(TbmAiParam param);

    /** POST /chat-item {mtrlItemCd,userMessage} — 대화형 보완(VLM, 이미지 매요청 재부착). */
    TbmAiItemResponse chatItem(TbmAiParam param);

    /** POST /confirm-item {mtrlItemCd,confirmText?} — 확정(LLM 미호출, 멱등). */
    TbmAiItemResponse confirmItem(TbmAiParam param);

    /** POST /reanalyze {mtrlItemCd,adminNote?} — 재분석 재진입(ANALYZING + 비동기 트리거). */
    TbmAiItemResponse reanalyze(TbmAiParam param);

    /** POST /manual-confirm {mtrlItemCd,manualText} — 실패 항목 수기 확정(LLM 미호출). */
    TbmAiItemResponse manualConfirm(TbmAiParam param);

    /** GET /analysis-worklist — AI 분석 워크리스트(회사/사업장 스코프 + 검색/필터/페이징, 읽기전용). */
    TbmAiWorklistResponse worklist(TbmAiWorklistParam param);
}
