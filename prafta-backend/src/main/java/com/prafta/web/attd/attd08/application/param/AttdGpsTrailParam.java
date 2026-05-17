package com.prafta.web.attd.attd08.application.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd08.dto.request.AttdGpsTrailRequest;

public record AttdGpsTrailParam(
      String attdId
    , String gvCmpnyCd
) {

    private static final Logger log = LoggerFactory.getLogger(AttdGpsTrailParam.class);

    public static AttdGpsTrailParam from(AttdGpsTrailRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            log.warn("AttdGpsTrailParam.from - request is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            log.warn("AttdGpsTrailParam.from - tokenInfo is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        if (request.getAttdId() == null || request.getAttdId().isBlank()) {
            log.warn("AttdGpsTrailParam.from - required field missing: attdId");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // ATTD_ID is varchar(20) on the DB side but the application format is a
        // numeric sequence (yyyyMMdd + seq). Reject non-numeric input early so
        // we never bind arbitrary strings to downstream queries.
        try {
            Long.parseLong(request.getAttdId());
        } catch (NumberFormatException e) {
            log.warn("AttdGpsTrailParam.from - attdId is not a valid numeric identifier. attdId={}",
                    request.getAttdId());
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new AttdGpsTrailParam(
              request.getAttdId()
            , tokenInfo.gv_cmpnyCd()
        );
    }
}
