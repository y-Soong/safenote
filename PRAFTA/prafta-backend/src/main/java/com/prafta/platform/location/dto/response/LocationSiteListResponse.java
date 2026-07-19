package com.prafta.platform.location.dto.response;

import java.util.List;

import com.prafta.platform.location.application.result.LocationSiteResult;

import lombok.Builder;
import lombok.Value;

/**
 * 대상 회사 사업장 목록 응답 DTO (GET /platformApi/location/site-lists).
 */
@Value
@Builder
public class LocationSiteListResponse {

    /** 사업장 목록(좌표/지오펜스 반경 포함 — 시설 좌표, 개인위치정보 아님). */
    List<LocationSiteResult> siteList;
}
