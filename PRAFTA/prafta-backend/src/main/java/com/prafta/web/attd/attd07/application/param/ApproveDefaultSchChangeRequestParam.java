package com.prafta.web.attd.attd07.application.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.dto.request.ApproveDefaultSchChangeRequestRequest;

/**
 * Server-internal carrier for POST /attd07/approve-default-sch-requests (PRAFTA-003, 기본근무타입-승인제).
 *
 * <p>{@link ApproveSchedModifyRequestParam}과 동일 골격이나 workYmd/workSeq/nodeCd 필드가 없다 —
 * 기본 근무타입 변경 요청은 특정 근무일에 종속되지 않기 때문이다.
 *
 * <p>canonical 생성자를 그대로 노출한다(record 기본) — 앱 관리자 승인 인박스
 * (AppAdminApprovalServiceImpl)가 body DTO 검증을 우회하고 직접 이 레코드를 구성해 호출하는
 * 선례({@code approveSchedModifyRequest} 호출부와 동일 패턴)를 재사용하기 위함이다.
 */
public record ApproveDefaultSchChangeRequestParam(
      String reqId
    , String siteCd
    , String userCd

    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
) {

    private static final Logger log = LoggerFactory.getLogger(ApproveDefaultSchChangeRequestParam.class);

    public static ApproveDefaultSchChangeRequestParam from(
            ApproveDefaultSchChangeRequestRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            log.warn("ApproveDefaultSchChangeRequestParam.from - request is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            log.warn("ApproveDefaultSchChangeRequestParam.from - tokenInfo is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            log.warn("ApproveDefaultSchChangeRequestParam.from - missing claim: gv_cmpnyCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            log.warn("ApproveDefaultSchChangeRequestParam.from - missing claim: gv_userCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // SEC-015 - 매니저 게이트는 JWT 기반 gvAuthCd 로 수행하므로 필수 운반.
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            log.warn("ApproveDefaultSchChangeRequestParam.from - missing claim: gv_authCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // SEC-017 - cross-site IDOR 가드를 위해 JWT site scope 필수 운반.
        if (tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            log.warn("ApproveDefaultSchChangeRequestParam.from - missing claim: gv_siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getReqId() == null || request.getReqId().isEmpty()) {
            log.warn("ApproveDefaultSchChangeRequestParam.from - required field missing: reqId");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getSiteCd() == null || request.getSiteCd().isEmpty()) {
            log.warn("ApproveDefaultSchChangeRequestParam.from - required field missing: siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // SEC-017 - cross-site IDOR 가드는 서비스 계층 SiteAccessService.assertSiteAccess 로 이관.

        return new ApproveDefaultSchChangeRequestParam(
              request.getReqId()
            , request.getSiteCd()
            , request.getUserCd()

            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
