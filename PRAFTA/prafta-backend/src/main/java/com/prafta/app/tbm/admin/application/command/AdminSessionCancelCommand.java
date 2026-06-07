package com.prafta.app.tbm.admin.application.command;

import com.prafta.app.tbm.admin.application.param.AdminSessionCancelParam;

/** TB_TBM_SESSION 취소(STATUS_CD='CANCELLED') UPDATE 커맨드. */
public record AdminSessionCancelCommand(
    String sessionCd
    , String cancelReason
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminSessionCancelCommand from(AdminSessionCancelParam param) {

        return new AdminSessionCancelCommand(
            param.sessionCd()
            , param.cancelReason() != null ? param.cancelReason().trim() : null
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
