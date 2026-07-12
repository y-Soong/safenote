package com.prafta.web.tbm.tbmai01.application.model;

/**
 * analysis-status 목록 1행(selectItemStatuses 결과).
 *
 * <p>★MyBatis 위치매핑: 필드 순서는 {@code TbmAi01Mapper.selectItemStatuses} 의 SELECT 컬럼(AS 별칭)
 *    순서와 반드시 일치시킨다. 모든 필드 String(datetime 은 {@code DATE_FORMAT} 문자열로 수신).
 */
public record TbmAiItemStatusRow(
    String mtrlItemCd
    , String mtrlItemType
    , String aiAnalyzeYn
    , String aiStatus
    , String aiDraftText
    , String aiConfirmDesc
    , String aiChatJson
    , String aiAnalyzedAt
) {}
