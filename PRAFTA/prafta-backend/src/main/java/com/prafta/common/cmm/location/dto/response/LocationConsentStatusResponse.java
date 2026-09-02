package com.prafta.common.cmm.location.dto.response;

import com.prafta.common.cmm.location.result.LocationConsentStatusResult;

import lombok.Builder;
import lombok.Value;

/**
 * 위치정보 동의 상태 응답 — 위치정보 동의철회·중지 S3.
 *
 * <p>화면은 {@code consentState} 로 버튼 구성을 결정한다.
 * <pre>
 *   AGREED           → [일시 중지] [동의 철회]
 *   SUSPENDED        → [재동의]  (+ "과거 기록은 그대로 있습니다")
 *   PENDING_REAGREE  → [재동의]  (+ "약관이 변경되어 다시 동의가 필요합니다")
 *   WITHDRAWN        → [재동의]  (+ "이전 위치정보는 삭제되었습니다")
 * </pre>
 */
@Value
@Builder
public class LocationConsentStatusResponse {

    /** AGREED / SUSPENDED / PENDING_REAGREE / WITHDRAWN */
    String consentState;

    /** 현재 시행 중인 위치기반서비스 약관 버전(재동의 시 응답해야 할 버전) */
    String termsVersion;

    /** 위치정보 수집 허용 여부(AGREED 일 때만 true) */
    boolean collectAllowed;

    /** 이번 요청으로 파기된 좌표 행 수(철회 응답에서만 의미 있음) */
    int purgedRows;

    /** 약관ID(005) — 화면의 [보기] 버튼이 약관 전문 화면으로 넘긴다. */
    String termsId;

    /** 약관명(SYS008) — 앱에 문구를 하드코딩하지 않도록 서버가 내린다. */
    String termsNm;

    public static LocationConsentStatusResponse of(LocationConsentStatusResult result) {
        return LocationConsentStatusResponse.builder()
                .consentState(result.consentState())
                .termsVersion(result.termsVersion())
                .collectAllowed(result.collectAllowed())
                .purgedRows(result.purgedRows())
                .termsId(result.termsId())
                .termsNm(result.termsNm())
                .build();
    }
}
