package com.prafta.web.acct.acct01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.application.param.AcctCreateParam;

/**
 * 사고 등록 INSERT 커맨드. acctId 는 서비스에서 채번 후 주입.
 * PROCESS_STATUS_CD 는 접수(100) 고정(DDL DEFAULT 와 동일).
 */
public record AcctInsertCommand(
    String siteCd
    , String acctId
    , String victimUserTypeCd
    , String victimUserCd
    , String occurYmd
    , String occurTime
    , String occurPlace
    , String acctGradeCd
    , String acctDesc
    , String employerDesc
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AcctInsertCommand from(AcctCreateParam param, String acctId) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (acctId == null || acctId.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AcctInsertCommand(
            param.siteCd()
            , acctId
            , param.victimUserTypeCd()
            , param.victimUserCd()
            , param.occurYmd()
            , param.occurTime()
            , param.occurPlace()
            , param.acctGradeCd()
            , param.acctDesc()
            , param.employerDesc()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
