package com.prafta.app.tbm.admin.result;

/**
 * R3 진행화면 슬라이드용 자료 세부항목(사용자 TBM tbm01 selectSessionContentItems 포팅).
 *
 * <p>출처 테이블 TB_TBM_EDU_MTRL_ITEM:
 *   itemType=MTRL_ITEM_TYPE[SYS018], fileMgmtCd=FILE_MGMT_CD, url=URL,
 *   itemDesc=MTRL_DESC, sortIdx=SORT_IDX. mtrlCd 는 서비스에서 묶음 그룹핑용.
 */
public record AdminContentItemResult(
    String mtrlCd
    , String mtrlItemCd
    , String itemType
    , String fileMgmtCd
    , String url
    , String itemDesc
    , Integer sortIdx
    , String filePath          // TB_FILE_INFO.FILE_PATH (서명 relPath 조립용, 파일 없으면 NULL)
    , String fileExt           // TB_FILE_INFO.FILE_EXT  (확장자, 점 포함)
){
}
