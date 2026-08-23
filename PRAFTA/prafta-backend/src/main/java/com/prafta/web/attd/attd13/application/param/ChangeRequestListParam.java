package com.prafta.web.attd.attd13.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd13.dto.request.ChangeRequestListRequest;

/**
 * 연차 변경 요청 목록 조회 Param (PRAFTA-COM-008-C).
 *
 * <p>회사/권한/사용자 식별은 토큰에서만 도출한다. siteCd 는 클라 지정값을 그대로 담으며,
 * 접근 가능 여부 검증(IDOR 가드)은 서비스 계층 {@code Attd13ServiceImpl.resolveSiteCds}
 * ({@code SiteAccessService.assertSiteAccess})가 수행한다(접수함연차변경다중사업장확장-001).
 */
public record ChangeRequestListParam(
      String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String userNm
    , String reqStatus
    , String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
    , String gvSiteCd
) {
    public static ChangeRequestListParam from(ChangeRequestListRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()
                || tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // 접수함연차변경다중사업장확장-001: 사업장 목록 해석(전사/접근가능 목록 vs 단건 지정)은
        //   서비스 계층 resolveSiteCds 로 이동했다 — siteCd 는 클라 지정값을 그대로 담아 전달한다
        //   (지정값이 있으면 SiteAccessService.assertSiteAccess 로 IDOR 재검증 후 그 1건, 없으면
        //   접근 가능 사업장 전체를 서비스가 조회한다).
        return new ChangeRequestListParam(
              request.getSITE_CD()
            , request.getNODE_CD()
            , "Y".equals(request.getINC_SUB_NODE_YN()) ? "Y" : "N"
            , request.getUSER_NM()
            , request.getREQ_STATUS()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
