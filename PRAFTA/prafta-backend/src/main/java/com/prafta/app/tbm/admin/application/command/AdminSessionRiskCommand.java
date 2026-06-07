package com.prafta.app.tbm.admin.application.command;

import com.prafta.app.tbm.admin.application.model.AdminSessionRiskModel;

/** TB_TBM_SESSION_RISK INSERT 커맨드(세션-위험성평가 매핑). */
public record AdminSessionRiskCommand(
    String sessionCd
    , String siteCd
    , String processCd
    , String assessmentCd
    , int displayOrder
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminSessionRiskCommand from(
            AdminSessionRiskModel model, String sessionCd, int displayOrder,
            String gvCmpnyCd, String gvUserCd) {

        return new AdminSessionRiskCommand(
            sessionCd
            , model.getSiteCd()
            , model.getProcessCd()
            , model.getAssessmentCd()
            , model.getDisplayOrder() != null ? model.getDisplayOrder() : displayOrder
            , gvCmpnyCd
            , gvUserCd
        );
    }
}
