package com.prafta.web.location.location01.application.param;

import com.prafta.common.dto.TokenInfo;

/**
 * 위치정보 동의 현황 목록 조회 Param — Location_01.
 *
 * <p>★식별값(cmpnyCd/userCd/authCd/nodeCd)은 전부 JWT 클레임에서만 도출한다.
 * 조회 범위를 넓히는 값을 요청 본문/쿼리로 받지 않는다.
 */
public record LocationConsentStatusParam(
        String siteCd
        , String stateFilter
        , String gvCmpnyCd
        , String gvUserCd
        , String gvAuthCd
        , String gvSiteCd
        , String gvNodeCd
) {
    public static LocationConsentStatusParam of(String siteCd, String stateFilter, TokenInfo t) {
        return new LocationConsentStatusParam(
                siteCd, stateFilter
                , t.gv_cmpnyCd(), t.gv_userCd(), t.gv_authCd(), t.gv_siteCd(), t.gv_nodeCd());
    }
}
