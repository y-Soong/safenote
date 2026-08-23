package com.prafta.common.cmm.siteauth.result;

/**
 * 접근 가능 사업장 1건(사업장 코드/번호/명).
 *
 * <p>{@link com.prafta.common.cmm.siteauth.service.SiteAccessService#getAccessibleSites}
 * 결과 타입. {@code com.prafta.app.admin.access.result.AccessibleSiteResult}(앱 전용,
 * 현장전환 셀렉터)와 필드 shape 만 동일하고 계층은 분리한다 — web/app 계층 결합 방지.
 */
public record AccessibleSiteResult(
      String siteCd
    , String siteNo
    , String siteNm
) {
}
