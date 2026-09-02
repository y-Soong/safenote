package com.prafta.web.location.location01.dto.response;

import java.util.List;

import com.prafta.web.location.location01.result.LocationConsentHistRow;
import com.prafta.web.location.location01.result.LocationPurgeHistRow;

import lombok.Builder;
import lombok.Value;

/**
 * 위치정보 동의 상세(이력) 응답 — Location_01.
 *
 * <p>전이 이력과 파기 이력을 함께 내린다 — 관리자가 "상태가 바뀌었다"와
 * "그래서 무엇을 지웠다"를 한 화면에서 대조할 수 있어야 책임 추궁이 성립한다.
 */
@Value
@Builder
public class LocationConsentHistResponse {
    List<LocationConsentHistRow> consentHistList;
    List<LocationPurgeHistRow> purgeHistList;
}
