package com.prafta.web.leave.promotion.leavepromo01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.PromotionFirstTargetSearchRequest;

/**
 * 연차 사용촉진 1차 현황 조회 Param(작업지시서_연차촉진-1차현황-화면-및-배치활성화 §5-1).
 *
 * <p>{@code PromotionTargetSearchParam}(2차) 검증 로직을 그대로 미러한다 — request/tokenInfo null,
 * siteCd blank, gv_cmpnyCd/gv_siteCd blank 면 COMMON_400_001. 1년차 필터는 없다.
 *
 * <p>cross-site IDOR 가드는 서비스 계층 {@code SiteAccessService.assertSiteAccess}(사업장 권한 원장
 * TB_USER_SITE_AUTH 기반 인가)가 담당한다. 노드 권한 게이팅(canManageNode)도 서비스 진입부에서 강제.
 */
public record PromotionFirstTargetSearchParam(
        String siteCd,
        String nodeCd,
        String incSubNodeYn,
        String userNm,
        String gvCmpnyCd,
        String gvAuthCd,
        String gvUserCd,
        String gvSiteCd
) {
    public static PromotionFirstTargetSearchParam from(PromotionFirstTargetSearchRequest request,
                                                      TokenInfo tokenInfo) {
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
        return new PromotionFirstTargetSearchParam(
                request.getSiteCd(),
                request.getNodeCd(),
                request.getIncSubNodeYn() == null ? "N" : request.getIncSubNodeYn(),
                request.getUserNm() == null ? "" : request.getUserNm(),
                tokenInfo.gv_cmpnyCd(),
                tokenInfo.gv_authCd(),
                tokenInfo.gv_userCd(),
                tokenInfo.gv_siteCd()
        );
    }
}
