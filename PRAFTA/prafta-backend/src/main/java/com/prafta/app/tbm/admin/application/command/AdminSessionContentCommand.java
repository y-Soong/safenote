package com.prafta.app.tbm.admin.application.command;

import com.prafta.app.tbm.admin.application.model.AdminSessionContentModel;

/** TB_TBM_SESSION_CONTENT INSERT 커맨드(세션-콘텐츠 묶음 매핑). */
public record AdminSessionContentCommand(
    String sessionCd
    , String mtrlCd
    , int displayOrder
    , String overrideDesc
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminSessionContentCommand from(
            AdminSessionContentModel model, String sessionCd, int displayOrder,
            String gvCmpnyCd, String gvUserCd) {

        return new AdminSessionContentCommand(
            sessionCd
            , model.getMtrlCd()
            , model.getDisplayOrder() != null ? model.getDisplayOrder() : displayOrder
            , model.getOverrideDesc()
            , gvCmpnyCd
            , gvUserCd
        );
    }
}
