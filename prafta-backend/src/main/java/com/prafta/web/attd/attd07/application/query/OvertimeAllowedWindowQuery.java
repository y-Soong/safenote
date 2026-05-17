package com.prafta.web.attd.attd07.application.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.application.param.UpdateUserOvertimeRequestParam;

/**
 * Query for {@code Attd07Mapper.selectAllowedWindow} - returns the scheduled
 * and standardized work intervals required to compute the "allowed OT window"
 * (standardized minus scheduled).
 */
public record OvertimeAllowedWindowQuery(
      String gvCmpnyCd
    , String siteCd
    , String userCd
    , String workYmd
) {

    private static final Logger log = LoggerFactory.getLogger(OvertimeAllowedWindowQuery.class);

    public static OvertimeAllowedWindowQuery from(UpdateUserOvertimeRequestParam param) {

        // SEC-020 - log the internal field name server-side only; do not leak the
        // precise missing field name in the API response.
        if (param == null) {
            log.warn("OvertimeAllowedWindowQuery.from - required param missing: UpdateUserOvertimeRequestParam");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new OvertimeAllowedWindowQuery(
              param.gvCmpnyCd()
            , param.siteCd()
            , param.userCd()
            , param.workYmd()
        );
    }
}
