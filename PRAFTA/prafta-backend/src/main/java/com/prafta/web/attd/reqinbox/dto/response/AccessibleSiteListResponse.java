package com.prafta.web.attd.reqinbox.dto.response;

import java.util.List;

import com.prafta.common.cmm.siteauth.result.AccessibleSiteResult;

import lombok.Builder;
import lombok.Value;

/**
 * 요청 승인 관리 — 접근 가능 사업장 목록 응답 (접수함다중사업장권한확장-002).
 *
 * <p>프론트 사업장 셀렉터(ReqInboxSiteFilter) 옵션 채우기용. 접근 판정은
 * {@code SiteAccessService.getAccessibleSites} 결과를 그대로 노출한다.
 */
@Value
@Builder
public class AccessibleSiteListResponse {
    List<AccessibleSiteResult> accessibleSites;
}
