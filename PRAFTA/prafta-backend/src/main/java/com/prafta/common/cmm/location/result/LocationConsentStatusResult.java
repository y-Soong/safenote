package com.prafta.common.cmm.location.result;

import com.prafta.common.cmm.location.LocationConsentConst;

/**
 * 위치정보 동의(005) 현재 상태 — 위치정보 동의철회·중지 S3.
 *
 * @param consentState  4-state({@code AGREED / SUSPENDED / PENDING_REAGREE / WITHDRAWN})
 * @param termsVersion  현재 시행 중인 약관 버전(응답해야 할 버전)
 * @param collectAllowed 위치정보 수집 허용 여부({@code AGREED} 일 때만 true)
 * @param purgedRows    이번 호출로 파기된 좌표 행 수(철회 응답에서만 의미. 그 외 0)
 */
public record LocationConsentStatusResult(
        String consentState
        , String termsVersion
        , boolean collectAllowed
        , int purgedRows
) {
    public static LocationConsentStatusResult of(String consentState, String termsVersion) {
        return of(consentState, termsVersion, 0);
    }

    public static LocationConsentStatusResult of(String consentState, String termsVersion, int purgedRows) {
        return new LocationConsentStatusResult(
                consentState
                , termsVersion
                , LocationConsentConst.STATE_AGREED.equals(consentState)
                , purgedRows);
    }
}
