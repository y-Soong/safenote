package com.prafta.web.location.location01.dto.response;

import java.util.List;

import com.prafta.web.location.location01.result.LocationConsentStatusRow;

import lombok.Builder;
import lombok.Value;

/** 위치정보 동의 현황 목록 응답 — Location_01. */
@Value
@Builder
public class LocationConsentStatusResponse {
    List<LocationConsentStatusRow> consentStatusList;
}
