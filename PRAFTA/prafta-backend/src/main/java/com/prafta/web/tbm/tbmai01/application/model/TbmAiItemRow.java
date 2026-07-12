package com.prafta.web.tbm.tbmai01.application.model;

/**
 * 단건 항목 AI 상태 1행(액션 EP 공용 — selectItemAiRow 결과).
 *
 * <p>★MyBatis 위치매핑: 필드 순서는 {@code TbmAi01Mapper.selectItemAiRow} 의 SELECT 컬럼(AS 별칭) 순서와
 *    반드시 일치시킨다. 모든 필드 String(datetime 은 {@code DATE_FORMAT} 문자열로 수신).
 */
public record TbmAiItemRow(
    String mtrlItemCd
    , String mtrlCd
    , String mtrlItemType
    , String fileMgmtCd
    , String aiAnalyzeYn
    , String aiStatus
    , String aiConfirmDesc
    , String aiDraftText
    , String aiChatJson
    , String aiAnalyzedAt
    , String siteCd     // M.SITE_CD (자료 사업장, 비어있음=회사공통) — 사업장 격리 게이트용
) {}
