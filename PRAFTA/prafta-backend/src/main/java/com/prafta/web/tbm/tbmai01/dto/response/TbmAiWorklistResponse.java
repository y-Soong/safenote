package com.prafta.web.tbm.tbmai01.dto.response;

import java.util.List;

/**
 * TBM AI 분석 워크리스트 응답(회사/사업장 스코프 목록 + 페이징 메타).
 *
 * <p>파일형 항목의 서명 URL(fileUrl)은 서비스가 {@code FileUrlSigner} 로 후처리 주입한다.
 */
public record TbmAiWorklistResponse(
    List<Item> items
    , int totalCount
    , int page
    , int size
) {
    /** 워크리스트 1건(자료명 조인 + 항목 AI 상태 + 파일/링크 정보). */
    public record Item(
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
        , String fileUrl
        , String url
        , String thumbFileMgmtCd
        , String durationSec
        , String aiAnalyzedAt
    ) {}

    public static TbmAiWorklistResponse of(List<Item> items, int totalCount, int page, int size) {
        return new TbmAiWorklistResponse(items, totalCount, page, size);
    }
}
