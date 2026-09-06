package com.prafta.web.acct.acct01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.AcctVictimItem;

/**
 * 사고 재해자 INSERT 커맨드(tb_acct_victim). victimSeq 는 서비스가 채번 후 주입.
 */
public record AcctVictimInsertCommand(
    String gvCmpnyCd
    , String siteCd
    , String acctId
    , Integer victimSeq
    , String userTypeCd
    , String userCd
    , String victimResultCd
    , Integer careDays
    , Integer restDays
    , String injuryPart
    , String injuryDesc
    , String gvUserCd
){
    public static AcctVictimInsertCommand from(
            String gvCmpnyCd, String siteCd, String acctId, Integer victimSeq,
            AcctVictimItem item, String gvUserCd) {

        if (item == null || victimSeq == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AcctVictimInsertCommand(
            gvCmpnyCd
            , siteCd
            , acctId
            , victimSeq
            , item.getUserTypeCd()
            , item.getUserCd()
            , item.getVictimResultCd()
            , item.getCareDays()
            , item.getRestDays()
            , item.getInjuryPart()
            , item.getInjuryDesc()
            , gvUserCd
        );
    }
}
