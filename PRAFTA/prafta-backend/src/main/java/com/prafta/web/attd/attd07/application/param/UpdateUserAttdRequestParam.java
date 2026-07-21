package com.prafta.web.attd.attd07.application.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.dto.request.UpdateUserAttdRequestRequest;

public record UpdateUserAttdRequestParam(
    String reqId
    , String targetId
    , String siteCd
    , String userCd
    , String workYmd
    , String workSeq
    , String nodeCd

    , String checkInDate
    , String checkInTime
    , String checkInMethod
    , String checkOutDate
    , String checkOutTime
    , String checkOutMethod

    , String oriCheckInDate
    , String oriCheckInTime
    , String oriCheckOutDate
    , String oriCheckOutTime

    , String processComment

    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
) {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserAttdRequestParam.class);

    public static UpdateUserAttdRequestParam from(UpdateUserAttdRequestRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            log.warn("UpdateUserAttdRequestParam.from - request is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        if (tokenInfo == null) {
            log.warn("UpdateUserAttdRequestParam.from - tokenInfo is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getReqId() == null || request.getReqId().isEmpty()) {
            log.warn("UpdateUserAttdRequestParam.from - required field missing: reqId");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // [보안 재작업] SEC-015 - 매니저 게이트는 JWT 기반 gvAuthCd 로 수행하므로 필수 운반.
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            log.warn("UpdateUserAttdRequestParam.from - missing claim: gv_authCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new UpdateUserAttdRequestParam(
            request.getReqId()
            , request.getAttdId()
            , request.getSiteCd()
            , request.getUserCd()
            , request.getWorkYmd()
            , request.getWorkSeq()
            , request.getNodeCd()

            , request.getCheckInDate()
            , request.getCheckInTime()
            , request.getCheckInMethod()
            , request.getCheckOutDate()
            , request.getCheckOutTime()
            , request.getCheckOutMethod()

            , request.getOriCheckInDate()
            , request.getOriCheckInTime()
            , request.getOriCheckOutDate()
            , request.getOriCheckOutTime()

            , request.getProcessComment()

            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
