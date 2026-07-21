package com.prafta.web.leave.promotion.leavepromo01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.PromotionTargetSearchRequest;

/**
 * prafta-com-008-A-4: 2차 회사직권 대상자 조회 Param.
 *
 * <p>Attd_12 FraudAttdSuspectParam 패턴. siteCd 는 세션 고정 사업장(JWT gv_siteCd)과 일치해야 한다
 * (cross-site IDOR 가드). 노드 관리 권한 게이팅(canManageNode)은 서비스 진입부에서 강제한다.
 */
public record PromotionTargetSearchParam(
        String siteCd,
        String nodeCd,
        String incSubNodeYn,
        String userNm,
        String tenureFilter,
        String gvCmpnyCd,
        String gvAuthCd,
        String gvUserCd,
        String gvSiteCd
) {
    public static PromotionTargetSearchParam from(PromotionTargetSearchRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getSiteCd() == null || request.getSiteCd().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // cross-site IDOR 가드는 서비스 계층 SiteAccessService.assertSiteAccess 로 이관
        //   (사업장 권한 원장 TB_USER_SITE_AUTH 기반 인가).
        String resolved = request.resolveTenure();
        String tenure = (resolved == null || resolved.isBlank()) ? "ALL" : resolved;
        return new PromotionTargetSearchParam(
                request.getSiteCd(),
                request.getNodeCd(),
                request.getIncSubNodeYn() == null ? "N" : request.getIncSubNodeYn(),
                request.getUserNm() == null ? "" : request.getUserNm(),
                tenure,
                tokenInfo.gv_cmpnyCd(),
                tokenInfo.gv_authCd(),
                tokenInfo.gv_userCd(),
                tokenInfo.gv_siteCd()
        );
    }
}
