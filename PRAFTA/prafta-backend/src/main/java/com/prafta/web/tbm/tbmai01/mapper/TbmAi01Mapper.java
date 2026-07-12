package com.prafta.web.tbm.tbmai01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.tbm.tbmai01.application.model.TbmAiAnalyzeTarget;
import com.prafta.web.tbm.tbmai01.application.model.TbmAiItemRow;
import com.prafta.web.tbm.tbmai01.application.model.TbmAiItemStatusRow;
import com.prafta.web.tbm.tbmai01.application.model.TbmAiMtrlScope;
import com.prafta.web.tbm.tbmai01.application.model.TbmAiWorklistRow;
import com.prafta.web.tbm.tbmai01.application.query.TbmAiWorklistQuery;

/**
 * TBM 세부항목 AI 분석·확정 매퍼.
 *
 * <p>★모든 항목 UPDATE/SELECT 는 {@code TB_TBM_EDU_MTRL} 조인으로 회사 스코프(IDOR)를 강제한다
 *    (항목 테이블 {@code TB_TBM_EDU_MTRL_ITEM} 에 CMPNY_CD 없음 → 자료 마스터에서 회사 도출).
 *    타 회사 코드를 알아도 {@code M.CMPNY_CD} 불일치로 0행 처리된다.
 */
@Mapper
public interface TbmAi01Mapper {

    /** 항목→자료→회사 스코프 도출. 미소유/미존재면 NULL(호출부 404). */
    String selectMtrlCdByItemCd(@Param("mtrlItemCd") String mtrlItemCd, @Param("gvCmpnyCd") String gvCmpnyCd);

    /**
     * 자료 소유 확인 + 사업장 스코프 도출(analyze-items/analysis-status 진입 게이트).
     * 미소유/미존재면 NULL(호출부 AI_404_002). 반환 siteCd 로 사업장 격리 게이트를 수행한다.
     */
    TbmAiMtrlScope selectMtrlSiteScope(@Param("mtrlCd") String mtrlCd, @Param("gvCmpnyCd") String gvCmpnyCd);

    /**
     * 사업장 격리: 사용자가 대상 사업장(SITE_CD) 권한(tb_user_site_auth USE_YN='Y')을 보유하는지 카운트.
     * >0 이면 접근 허용(T2 TbmAi02Mapper.countUserSiteAuth 와 동일 쿼리).
     */
    int countUserSiteAuth(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd,
                          @Param("siteCd") String siteCd);

    /** 큐잉 대상 조회(자료에 딸린 분석지정 이미지·PDF 항목 중 재큐잉 가능 상태). */
    List<TbmAiAnalyzeTarget> selectAnalyzeTargets(@Param("mtrlCd") String mtrlCd, @Param("gvCmpnyCd") String gvCmpnyCd);

    /** 큐잉 상태 전이(대상 항목 일괄 ANALYZING). 반환=영향행수. */
    int markItemsAnalyzing(@Param("mtrlCd") String mtrlCd, @Param("gvCmpnyCd") String gvCmpnyCd,
                           @Param("userCd") String userCd);

    /**
     * 저장 후 자동 큐잉 전용 대상 조회. {@link #selectAnalyzeTargets} 와 달리 DRAFT 를 제외한다.
     *
     * <p>관리자가 초안(DRAFT)을 검토·대화 중인 항목을, 무관한 저장 한 번으로 재분석해
     * 초안과 대화 이력을 덮어쓰는 사고를 막는다. DRAFT 재분석은 명시적 reanalyze EP 로만 수행한다.
     */
    List<TbmAiAnalyzeTarget> selectAutoAnalyzeTargets(@Param("mtrlCd") String mtrlCd,
                                                      @Param("gvCmpnyCd") String gvCmpnyCd);

    /** 자동 큐잉 상태 전이(DRAFT 제외 — selectAutoAnalyzeTargets 와 동일 WHERE). 반환=영향행수. */
    int markItemsAnalyzingAuto(@Param("mtrlCd") String mtrlCd, @Param("gvCmpnyCd") String gvCmpnyCd,
                               @Param("userCd") String userCd);

    /** 단건 ANALYZING 전이(reanalyze). */
    int markItemAnalyzing(@Param("mtrlItemCd") String mtrlItemCd, @Param("gvCmpnyCd") String gvCmpnyCd,
                          @Param("userCd") String userCd);

    /** 단건 항목 AI행 조회(액션 EP 공용). NULL 이면 미소유/미존재. */
    TbmAiItemRow selectItemAiRow(@Param("mtrlItemCd") String mtrlItemCd, @Param("gvCmpnyCd") String gvCmpnyCd);

    /** 분석 결과 저장(성공=DRAFT + 초안 + 대화 시드 + 분석일시). */
    int updateItemDraft(@Param("mtrlItemCd") String mtrlItemCd, @Param("draftText") String draftText,
                        @Param("chatJson") String chatJson, @Param("gvCmpnyCd") String gvCmpnyCd,
                        @Param("userCd") String userCd);

    /** 분석 실패 저장(FAILED). */
    int updateItemFailed(@Param("mtrlItemCd") String mtrlItemCd, @Param("gvCmpnyCd") String gvCmpnyCd,
                         @Param("userCd") String userCd);

    /** 대화이력 저장(chat-item, 텍스트만 — 상태 미변경). */
    int updateItemChatJson(@Param("mtrlItemCd") String mtrlItemCd, @Param("chatJson") String chatJson,
                           @Param("gvCmpnyCd") String gvCmpnyCd, @Param("userCd") String userCd);

    /** 확정 저장(confirm-item/manual-confirm — CONFIRM_DESC + CONFIRMED, 멱등). */
    int updateItemConfirm(@Param("mtrlItemCd") String mtrlItemCd, @Param("confirmDesc") String confirmDesc,
                          @Param("gvCmpnyCd") String gvCmpnyCd, @Param("userCd") String userCd);

    /** analysis-status 목록(자료 단위 항목별 AI 상태). */
    List<TbmAiItemStatusRow> selectItemStatuses(@Param("mtrlCd") String mtrlCd, @Param("gvCmpnyCd") String gvCmpnyCd);

    /** 분석 워크리스트 목록(회사/사업장 스코프 + 검색/필터 + 페이징). */
    List<TbmAiWorklistRow> selectAnalysisWorklist(TbmAiWorklistQuery query);

    /** 분석 워크리스트 총건수(페이징용, 동일 WHERE). */
    int countAnalysisWorklist(TbmAiWorklistQuery query);
}
