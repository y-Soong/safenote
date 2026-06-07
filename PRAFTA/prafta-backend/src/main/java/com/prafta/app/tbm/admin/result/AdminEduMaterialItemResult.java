package com.prafta.app.tbm.admin.result;

/**
 * R5 교육자료 상세 항목(편집/미리보기용 풀필드).
 *
 * <p>R3 의 {@code AdminContentItemResult}(슬라이드용 경량)와 별도로 둔다 — 자료 상세는
 * mtrlItemCd/fileMgmtCd/thumb/duration 등 편집 풀필드가 필요하다(계약 §4 ⚠️ 참조).
 *
 * <p>서명 URL 전환: 파일 서빙 url 은 SQL CONCAT 이 아니라 서비스에서 {@code FileUrlSigner} 로
 * 서명 발급한다. 따라서 본 결과는 원시 컬럼(filePath/fileExt)만 보유하고, 서명 previewUrl 은
 * 응답 DTO({@code AdminEduMaterialItemResponse})에서 채운다. 파일 없으면 filePath=NULL.
 */
public record AdminEduMaterialItemResult(
    String mtrlItemCd
    , String mtrlItemType      // SYS018: 01 이미지 / 02 동영상 / 03 유튜브URL / 04 PDF
    , String mtrlDesc
    , String fileMgmtCd
    , String thumbFileMgmtCd
    , Integer durationSec
    , String url
    , int sortIdx
    , String filePath          // TB_FILE_INFO.FILE_PATH (서명 relPath 조립용, 파일 없으면 NULL)
    , String fileExt           // TB_FILE_INFO.FILE_EXT  (확장자, 점 포함)
){
}
