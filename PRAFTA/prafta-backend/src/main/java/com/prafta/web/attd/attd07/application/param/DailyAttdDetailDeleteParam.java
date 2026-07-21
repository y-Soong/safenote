package com.prafta.web.attd.attd07.application.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.dto.request.DailyAttdDetailDeleteRequest;

/**
 * Server-internal carrier for POST /attd07/daily-attd-detail-delete.
 *
 * 검증된 요청 body 와 JWT 파생 신원(cmpny / user / authCd)을 결합한다.
 *
 * [보안 재작업] prafta-010-001 - PRAFTA-016 으로 본 endpoint 가 근태 + 연결 OT 연쇄
 * soft-delete 까지 수행하면서 파괴력이 커졌으므로, 형제 쓰기 endpoint
 * (rejectUserAttdRequest 등) 가 갖춘 SEC-015 매니저 게이트와 SEC-017 cross-site
 * IDOR 가드를 본 endpoint 에도 적용한다. 이를 위해 gvAuthCd 를 함께 운반하고,
 * from 팩토리에서 body siteCd ↔ JWT gv_siteCd 일치 및 필수 필드 검증을 수행한다.
 */
public record DailyAttdDetailDeleteParam(
    String siteCd
    , String userCd
    , String attdId
    , String reason
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
) {

    private static final Logger log = LoggerFactory.getLogger(DailyAttdDetailDeleteParam.class);

    public static DailyAttdDetailDeleteParam from(DailyAttdDetailDeleteRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            log.warn("DailyAttdDetailDeleteParam.from - request is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            log.warn("DailyAttdDetailDeleteParam.from - tokenInfo is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            log.warn("DailyAttdDetailDeleteParam.from - missing claim: gv_cmpnyCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            log.warn("DailyAttdDetailDeleteParam.from - missing claim: gv_userCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // [보안 재작업] SEC-015 - 매니저 게이트는 JWT 기반 gvAuthCd 로 수행하므로 필수 운반.
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            log.warn("DailyAttdDetailDeleteParam.from - missing claim: gv_authCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // [보안 재작업] SEC-017 - cross-site IDOR 가드를 위해 JWT site scope 필수.
        if (tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            log.warn("DailyAttdDetailDeleteParam.from - missing claim: gv_siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getSiteCd() == null || request.getSiteCd().isBlank()) {
            log.warn("DailyAttdDetailDeleteParam.from - required field missing: siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getUserCd() == null || request.getUserCd().isBlank()) {
            log.warn("DailyAttdDetailDeleteParam.from - required field missing: userCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getAttdId() == null || request.getAttdId().isBlank()) {
            log.warn("DailyAttdDetailDeleteParam.from - required field missing: attdId");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // SEC-017 - cross-site IDOR 가드는 서비스 계층 SiteAccessService.assertSiteAccess 로 이관
        //   (사업장 권한 원장 TB_USER_SITE_AUTH 기반 인가. 토큰 등식 강제는 다사업장 관리를 막았다).

        return new DailyAttdDetailDeleteParam(
            request.getSiteCd()
            , request.getUserCd()
            , request.getAttdId()
            , request.getReason()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
