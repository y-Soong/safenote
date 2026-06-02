package com.prafta.web.attd.attd07.application.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.dto.request.RejectUserOvertimeRequestRequest;

/**
 * Server-internal carrier for POST /attd07/reject-user-overtime-requests (PRAFTA-010).
 *
 * 검증된 요청 body 와 JWT 파생 신원(cmpny / user / authCd / siteCd)을 결합한다.
 * SEC-015 매니저 게이트와 SEC-017 cross-site IDOR 가드를 위해 gvAuthCd / gvSiteCd 를
 * 함께 운반한다.
 */
public record RejectUserOvertimeRequestParam(
      String reqId
    , String siteCd
    , String userCd
    , String rejectReason

    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
) {

    private static final Logger log = LoggerFactory.getLogger(RejectUserOvertimeRequestParam.class);

    public static RejectUserOvertimeRequestParam from(RejectUserOvertimeRequestRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            log.warn("RejectUserOvertimeRequestParam.from - request is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            log.warn("RejectUserOvertimeRequestParam.from - tokenInfo is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            log.warn("RejectUserOvertimeRequestParam.from - missing claim: gv_cmpnyCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            log.warn("RejectUserOvertimeRequestParam.from - missing claim: gv_userCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            log.warn("RejectUserOvertimeRequestParam.from - missing claim: gv_authCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            log.warn("RejectUserOvertimeRequestParam.from - missing claim: gv_siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getReqId() == null || request.getReqId().isEmpty()) {
            log.warn("RejectUserOvertimeRequestParam.from - required field missing: reqId");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getSiteCd() == null || request.getSiteCd().isEmpty()) {
            log.warn("RejectUserOvertimeRequestParam.from - required field missing: siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getRejectReason() == null || request.getRejectReason().isBlank()) {
            log.warn("RejectUserOvertimeRequestParam.from - required field missing: rejectReason");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // SEC-017 - cross-site IDOR guard.
        // body 가 호출자의 JWT site scope 와 다른 사업장을 지정하면 거부한다.
        if (!request.getSiteCd().equals(tokenInfo.gv_siteCd())) {
            log.warn("OT reject request site mismatch with token. requested={}, token={}",
                    request.getSiteCd(), tokenInfo.gv_siteCd());
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new RejectUserOvertimeRequestParam(
              request.getReqId()
            , request.getSiteCd()
            , request.getUserCd()
            , request.getRejectReason()

            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
