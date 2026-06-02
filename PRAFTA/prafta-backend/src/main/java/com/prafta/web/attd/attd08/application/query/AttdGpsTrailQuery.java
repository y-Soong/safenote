package com.prafta.web.attd.attd08.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd08.application.param.AttdGpsTrailParam;

public record AttdGpsTrailQuery(
      String attdId
    , String gvCmpnyCd
) {
    public static AttdGpsTrailQuery from(AttdGpsTrailParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AttdGpsTrailQuery(
              param.attdId()
            , param.gvCmpnyCd()
        );
    }
}
