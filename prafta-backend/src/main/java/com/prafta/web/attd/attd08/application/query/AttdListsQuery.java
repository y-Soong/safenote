package com.prafta.web.attd.attd08.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd08.application.param.AttdListsParam;

public record AttdListsQuery(
      String fromYmd
    , String toYmd
    , String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String userNm
    , String gvCmpnyCd
) {
    public static AttdListsQuery from(AttdListsParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AttdListsQuery(
              param.fromDate() == null ? null : param.fromDate().replace("-", "")
            , param.toDate()   == null ? null : param.toDate().replace("-", "")
            , param.siteCd()
            , param.nodeCd()
            , param.incSubNodeYn()
            , param.userNm()
            , param.gvCmpnyCd()
        );
    }
}
