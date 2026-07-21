package com.prafta.web.attd.attd07.application.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.dto.request.ApproveSchedModifyRequestRequest;

/**
 * Server-internal carrier for POST /attd07/approve-sched-modify-requests (PRAFTA-APP-007).
 *
 * <p>검증된 요청 body 와 JWT 파생 신원(cmpny / user / authCd / siteCd)을 결합한다.
 * SEC-012 준수를 위해 from 팩토리는 누락 필드명을 서버 로그에만 남기고
 * 클라이언트에는 일반 COMMON_400_001 만 노출한다.
 *
 * <p>SEC-015 매니저 게이트(gvAuthCd)와 SEC-017 cross-site IDOR 가드(gvSiteCd)를 위해
 * gvAuthCd / gvSiteCd 를 함께 운반한다 (RejectUserAttdRequestParam 동일 패턴).
 *
 * <p>목표 스케줄 코드(SCH_CD)는 운반하지 않는다 — 승인 시 서버가 REQ row 의 SCH_CD 를
 * 권위 값으로 사용한다.
 */
public record ApproveSchedModifyRequestParam(
      String reqId
    , String siteCd
    , String userCd
    , String workYmd
    , String workSeq
    , String nodeCd

    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
) {

    private static final Logger log = LoggerFactory.getLogger(ApproveSchedModifyRequestParam.class);

    public static ApproveSchedModifyRequestParam from(ApproveSchedModifyRequestRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            log.warn("ApproveSchedModifyRequestParam.from - request is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            log.warn("ApproveSchedModifyRequestParam.from - tokenInfo is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            log.warn("ApproveSchedModifyRequestParam.from - missing claim: gv_cmpnyCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            log.warn("ApproveSchedModifyRequestParam.from - missing claim: gv_userCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // SEC-015 - 매니저 게이트는 JWT 기반 gvAuthCd 로 수행하므로 필수 운반.
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            log.warn("ApproveSchedModifyRequestParam.from - missing claim: gv_authCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // SEC-017 - cross-site IDOR 가드를 위해 JWT site scope 필수 운반.
        if (tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            log.warn("ApproveSchedModifyRequestParam.from - missing claim: gv_siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getReqId() == null || request.getReqId().isEmpty()) {
            log.warn("ApproveSchedModifyRequestParam.from - required field missing: reqId");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getSiteCd() == null || request.getSiteCd().isEmpty()) {
            log.warn("ApproveSchedModifyRequestParam.from - required field missing: siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // SEC-017 - cross-site IDOR 가드는 서비스 계층 SiteAccessService.assertSiteAccess 로 이관
        //   (사업장 권한 원장 TB_USER_SITE_AUTH 기반 인가).

        return new ApproveSchedModifyRequestParam(
              request.getReqId()
            , request.getSiteCd()
            , request.getUserCd()
            , request.getWorkYmd()
            , request.getWorkSeq()
            , request.getNodeCd()

            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
