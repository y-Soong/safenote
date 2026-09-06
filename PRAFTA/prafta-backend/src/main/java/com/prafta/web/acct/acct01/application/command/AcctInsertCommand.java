package com.prafta.web.acct.acct01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.application.param.AcctCreateParam;
import com.prafta.web.acct.acct01.dto.request.AcctVictimItem;

/**
 * 사고 등록 INSERT 커맨드. acctId 는 서비스에서 채번 후 주입.
 * PROCESS_STATUS_CD 는 접수(100) 고정(DDL DEFAULT 와 동일).
 * 헤더 재해자 컬럼(VICTIM_USER_TYPE_CD/VICTIM_USER_CD) = 대표 재해자(배열 첫 인원, prafta-065 D1) — 서비스가 명시 전달.
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
    public static AcctInsertCommand from(AcctCreateParam param, String acctId, AcctVictimItem representative) {

        if (param == null || representative == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (acctId == null || acctId.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AcctInsertCommand(
            param.siteCd()
            , acctId
            , representative.getUserTypeCd()
            , representative.getUserCd()
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
