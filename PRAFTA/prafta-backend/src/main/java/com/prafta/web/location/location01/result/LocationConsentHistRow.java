package com.prafta.web.location.location01.result;

/**
 * 위치정보 동의 전이 이력 1행 — Location_01 상세.
 *
 * <p>★record 위치 매핑 — SELECT 순서 유지.
 *
 * @param beforeState 전이 전 상태(NULL=최초 응답 또는 상태 도입 이전 행)
 * @param agrSource   응답 경로[GATE/MYPAGE/JOIN/SYSTEM/ADMIN]
 */
public record LocationConsentHistRow(
      long histId
    , String termsVersion
    , String beforeState
    , String afterState
    , String beforeAgrYn
    , String afterAgrYn
    , String agrSource
    , String actorUserCd
    , String actionDtime
) {
}
