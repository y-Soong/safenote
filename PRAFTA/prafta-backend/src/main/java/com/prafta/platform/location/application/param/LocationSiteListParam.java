package com.prafta.platform.location.application.param;

import com.prafta.common.error.platform.PlatformErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.platform.location.dto.request.LocationSiteListRequest;

/**
 * 대상 회사 사업장 목록 조회 파라미터(site-lists).
 *
 * <p>cmpnyCd 파라미터는 신뢰한다 — sysadmin 전용 게이트 통과가 전제(전 회사 조회가 정당한 유일 주체).
 */
public record LocationSiteListParam(
    String cmpnyCd
) {
    public static LocationSiteListParam from(LocationSiteListRequest request) {

        if (request == null || isBlank(request.getCmpnyCd())) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_011);
        }

        return new LocationSiteListParam(request.getCmpnyCd().trim());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
