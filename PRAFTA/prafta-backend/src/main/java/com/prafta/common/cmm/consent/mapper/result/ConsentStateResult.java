package com.prafta.common.cmm.consent.mapper.result;

/**
 * 사용자의 특정 약관/버전 현재 동의값 + 동의상태 — 위치정보 동의철회·중지 S2.
 *
 * <p>★record 매핑이라 SELECT 컬럼 순서가 컴포넌트 순서와 같아야 한다(agrYn → consentState).
 *
 * @param agrYn        동의여부(행이 있으면 'Y'/'N'. 행 자체가 없으면 결과 없음 = null 반환)
 * @param consentState 4-state 동의상태. 상태 관리 대상이 아닌 약관이거나 승계 전 행이면 null
 */
public record ConsentStateResult(
        String agrYn
        , String consentState
) {
}
