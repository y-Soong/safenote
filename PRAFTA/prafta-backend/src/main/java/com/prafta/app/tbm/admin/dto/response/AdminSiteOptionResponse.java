package com.prafta.app.tbm.admin.dto.response;

import java.util.List;

import com.prafta.app.admin.access.result.AccessibleSiteResult;

import lombok.Builder;
import lombok.Getter;

/**
 * T-K 사업장 선택 옵션 응답.
 *
 * <p>access-context.accessibleSites 와 동일 소스(TB_USER_SITE_AUTH ⨝ TB_SITE, USE_YN='Y')를 재사용한다.
 */
@Getter
@Builder
public class AdminSiteOptionResponse {
    private List<AccessibleSiteResult> sites;
}
