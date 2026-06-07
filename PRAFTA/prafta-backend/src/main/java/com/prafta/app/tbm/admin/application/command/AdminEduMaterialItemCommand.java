package com.prafta.app.tbm.admin.application.command;

/**
 * R5 교육자료 항목 INSERT 커맨드(TB_TBM_EDU_MTRL_ITEM).
 *
 * <p>파일형(01/02/04)은 fileMgmtCd(신규 업로드 결과 또는 기존 유지), 외부링크형(03)은 url 을 채운다.
 *   thumbFileMgmtCd/durationSec 는 현재 미사용(NULL) — 자동 썸네일/길이추출은 본 라운드 범위 외.
 */
public record AdminEduMaterialItemCommand(
    String mtrlItemCd
    , String mtrlCd
    , int sortIdx
    , String mtrlItemType
    , String mtrlDesc
    , String fileMgmtCd
    , String thumbFileMgmtCd
    , Integer durationSec
    , String url
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminEduMaterialItemCommand of(String mtrlItemCd, String mtrlCd, int sortIdx,
            String mtrlItemType, String mtrlDesc, String fileMgmtCd, String url,
            String gvCmpnyCd, String gvUserCd) {

        return new AdminEduMaterialItemCommand(
            mtrlItemCd, mtrlCd, sortIdx, mtrlItemType, mtrlDesc, fileMgmtCd,
            null, null, url, gvCmpnyCd, gvUserCd);
    }
}
