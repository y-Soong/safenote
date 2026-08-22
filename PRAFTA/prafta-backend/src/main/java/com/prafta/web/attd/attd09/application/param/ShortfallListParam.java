package com.prafta.web.attd.attd09.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd09.dto.request.ShortfallListRequest;

/**
 * 입사일 기준 차액 조회 목록 Param (경력인정 이원화 Phase 2 §2-2).
 *
 * <p>GET endpoint. CMPNY_CD는 JWT에서만 취득(가드레일 3). 게이트는 {@code ensureManager}(master/hr
 * 전용 — P-13) + 사업장 필터 지정 시 {@code assertSiteAccess} 판정에 필요한 토큰 클레임(userCd/siteCd)을
 * 함께 운반한다.
 */
public record ShortfallListParam(
      String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
    , String gvSiteCd
    , String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String userNm
    , String baseYmd
    , int page
    , int size
) {

    public static ShortfallListParam from(ShortfallListRequest request, TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        ShortfallListRequest req = (request == null) ? new ShortfallListRequest() : request;
        int page = (req.getPage() == null) ? 1 : req.getPage();
        int size = (req.getSize() == null) ? 20 : req.getSize();
        String incSubNodeYn = "Y".equals(req.getIncSubNodeYn()) ? "Y" : "N";

        return new ShortfallListParam(
              tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , req.getSiteCd()
            , req.getNodeCd()
            , incSubNodeYn
            , req.getUserNm()
            , req.getBaseYmd()
            , page
            , size
        );
    }
}
