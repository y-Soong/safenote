package com.prafta.web.attd.attd07.application.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.dto.request.RejectUserAttdRequestRequest;

/**
 * Server-internal carrier for POST /attd07/reject-user-attd-requests (PRAFTA-008).
 *
 * 검증된 요청 body 와 JWT 파생 신원(cmpny / user / authCd / siteCd)을 결합한다.
 * SEC-012 준수를 위해 from 팩토리는 누락 필드명을 서버 로그에만 남기고
 * 클라이언트에는 일반 COMMON_400_001 만 노출한다.
 *
 * [보안 재작업] SEC-015 매니저 게이트와 SEC-017 cross-site IDOR 가드를 위해
 * gvAuthCd / gvSiteCd 를 함께 운반한다 (RejectUserOvertimeRequestParam 동일 패턴).
 */
public record RejectUserAttdRequestParam(
      String reqId
    , String siteCd
    , String userCd
    , String workYmd
    , String workSeq
    , String nodeCd
    , String rejectReason

    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
) {

    private static final Logger log = LoggerFactory.getLogger(RejectUserAttdRequestParam.class);

    public static RejectUserAttdRequestParam from(RejectUserAttdRequestRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            log.warn("RejectUserAttdRequestParam.from - request is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            log.warn("RejectUserAttdRequestParam.from - tokenInfo is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            log.warn("RejectUserAttdRequestParam.from - missing claim: gv_cmpnyCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            log.warn("RejectUserAttdRequestParam.from - missing claim: gv_userCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // [보안 재작업] SEC-015 - 매니저 게이트는 JWT 기반 gvAuthCd 로 수행하므로 필수 운반.
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            log.warn("RejectUserAttdRequestParam.from - missing claim: gv_authCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // [보안 재작업] SEC-017 - cross-site IDOR 가드를 위해 JWT site scope 필수 운반.
        if (tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            log.warn("RejectUserAttdRequestParam.from - missing claim: gv_siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getReqId() == null || request.getReqId().isEmpty()) {
            log.warn("RejectUserAttdRequestParam.from - required field missing: reqId");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getSiteCd() == null || request.getSiteCd().isEmpty()) {
            log.warn("RejectUserAttdRequestParam.from - required field missing: siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getRejectReason() == null || request.getRejectReason().isBlank()) {
            log.warn("RejectUserAttdRequestParam.from - required field missing: rejectReason");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // [보안 재작업] SEC-017 - cross-site IDOR guard.
        // body 가 호출자의 JWT site scope 와 다른 사업장을 지정하면 거부한다
        // (RejectUserOvertimeRequestParam 과 동일 패턴).
        if (!request.getSiteCd().equals(tokenInfo.gv_siteCd())) {
            log.warn("attd reject request site mismatch with token. requested={}, token={}",
                    request.getSiteCd(), tokenInfo.gv_siteCd());
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new RejectUserAttdRequestParam(
              request.getReqId()
            , request.getSiteCd()
            , request.getUserCd()
            , request.getWorkYmd()
            , request.getWorkSeq()
            , request.getNodeCd()
            , request.getRejectReason()

            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
