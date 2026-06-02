package com.prafta.app.chkLst.chkLst01.application.query;

import com.prafta.app.chkLst.chkLst01.application.param.ChecklistInfoParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-036-B1: 체크리스트 정보 조회 Query (mapper 진입).
 */
public record ChecklistInfoQuery(
    String cmpnyCd
    , String siteCd
    , String chkptCd
    , String chkptNm
) {
    public static ChecklistInfoQuery from(ChecklistInfoParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ChecklistInfoQuery(
            param.cmpnyCd()
            , param.siteCd()
            , param.chkptCd()
            , param.chkptNm()
        );
    }
}
