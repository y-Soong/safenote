package com.prafta.web.attd.attd07.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.dto.request.AttdCloseStatusRequest;

/**
 * 근태 마감 상태 조회 Param (prafta-019-C).
 */
public record AttdCloseStatusParam(
      String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String closeYm
    , String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
    , String gvSiteCd
) {
    public static AttdCloseStatusParam from(AttdCloseStatusRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getSiteCd() == null || request.getSiteCd().isBlank()
                || request.getCloseYm() == null || request.getCloseYm().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()
                || tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // cross-site IDOR 가드는 서비스 계층 SiteAccessService.assertSiteAccess 로 이관
        //   (사업장 권한 원장 TB_USER_SITE_AUTH 기반 인가. 조회도 사업장 스코프 강제 유지).
        return new AttdCloseStatusParam(
              request.getSiteCd()
            , request.getNodeCd()
            , request.getIncSubNodeYn()
            , request.getCloseYm()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
