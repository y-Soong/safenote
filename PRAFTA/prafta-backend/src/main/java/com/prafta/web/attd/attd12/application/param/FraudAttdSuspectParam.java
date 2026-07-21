package com.prafta.web.attd.attd12.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd12.dto.request.FraudAttdSuspectRequest;

/**
 * prafta-com-003 C6 - 부정 출퇴근 의심 조회 파라미터.
 *
 * <p>Attd_11 MonthlyAttdSummaryParam 패턴 동일. siteCd 는 세션 고정 사업장(JWT gv_siteCd)과
 * 일치해야 하며(cross-site IDOR 가드), 노드/사업장 관리 권한 게이팅은 서비스 진입부에서
 * canManageNode 로 강제한다(master/hr/safe 전사 또는 노드 관리자만 허용).
 */
public record FraudAttdSuspectParam(
        String workYm
        , String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String suspectType
        , String gvCmpnyCd
        , String gvAuthCd
        , String gvUserCd
        , String gvSiteCd
) {
    public static FraudAttdSuspectParam from(FraudAttdSuspectRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        if (request.getWorkYm() == null || request.getWorkYm().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (request.getSiteCd() == null || request.getSiteCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        // cross-site IDOR 가드는 서비스 계층 SiteAccessService.assertSiteAccess 로 이관
        //   (사업장 권한 원장 TB_USER_SITE_AUTH 기반 인가).

        return new FraudAttdSuspectParam(
                request.getWorkYm()
                , request.getSiteCd()
                , request.getNodeCd()
                , request.getIncSubNodeYn() == null ? "N" : request.getIncSubNodeYn()
                , request.getSuspectType() == null ? "" : request.getSuspectType()
                , tokenInfo.gv_cmpnyCd()
                , tokenInfo.gv_authCd()
                , tokenInfo.gv_userCd()
                , tokenInfo.gv_siteCd()
        );
    }
}
