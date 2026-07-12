package com.prafta.web.tbm.tbmai01.application.model;

/**
 * 분석 워크리스트 목록 1행(selectAnalysisWorklist 결과).
 *
 * <p>★MyBatis 위치매핑: 필드 순서는 {@code TbmAi01Mapper.selectAnalysisWorklist} 의 SELECT 컬럼(AS 별칭)
 *    순서와 반드시 일치시킨다. datetime 은 {@code DATE_FORMAT} 문자열로 수신. fileUrl(서명)은 서비스
 *    후처리로 응답 Item 에 주입하므로 본 row 에는 포함하지 않는다.
 */
public record TbmAiWorklistRow(
    String mtrlCd
    , String title
    , String siteCd
    , String mtrlItemCd
    , String mtrlItemType
    , String mtrlDesc
    , String aiAnalyzeYn
    , String aiStatus
    , String hasConfirmDesc
    , String fileMgmtCd
    , String fileNm
    , String filePath
    , String fileExt
    , String url
    , String thumbFileMgmtCd
    , String durationSec
    , String aiAnalyzedAt
) {}
