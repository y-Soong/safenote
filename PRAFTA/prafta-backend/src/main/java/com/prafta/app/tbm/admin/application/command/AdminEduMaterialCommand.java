package com.prafta.app.tbm.admin.application.command;

/**
 * R5 교육자료 묶음 INSERT/UPDATE 커맨드(TB_TBM_EDU_MTRL).
 *
 * <p>siteCd null=회사공통. 수정 시 SITE_CD(스코프)는 변경하지 않는다(web Tbm01 패리티 — IFNULL 보존).
 */
public record AdminEduMaterialCommand(
    String mtrlCd
    , String siteCd
    , String title
    , String contents
    , String mtrlType
    , String useYn
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminEduMaterialCommand of(String mtrlCd, String siteCd, String title,
            String contents, String mtrlType, String useYn, String gvCmpnyCd, String gvUserCd) {

        return new AdminEduMaterialCommand(
            mtrlCd, siteCd, title, contents, mtrlType, useYn, gvCmpnyCd, gvUserCd);
    }
}
