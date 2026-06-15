package com.prafta.web.risk.risklink01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risklink01.application.param.NearMissLinkParam;

/**
 * L3 연결 추가(upsert) / L4 연결 해제(soft delete) 공통 커맨드.
 */
public record NearMissLinkCommand(
    String gvCmpnyCd
    , String siteCd
    , String processCd
    , String assessmentCd
    , String nearMissId
    , String gvUserCd
){
    public static NearMissLinkCommand from(NearMissLinkParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new NearMissLinkCommand(
            param.gvCmpnyCd()
            , param.siteCd()
            , param.processCd()
            , param.assessmentCd()
            , param.nearMissId()
            , param.gvUserCd()
        );
    }
}
