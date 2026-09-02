package com.prafta.web.location.location01.result;

/**
 * 위치정보 동의 현황 1행 — Location_01 목록.
 *
 * <p>★record 위치 매핑이므로 {@code selectConsentStatusList} 의 SELECT 순서와
 * 아래 컴포넌트 순서를 반드시 일치시킬 것.
 *
 * @param userTypeCd   계정 계통(REGULAR/DAILY)
 * @param consentState 4-state. 현재버전 행이 없으면 PENDING_REAGREE
 * @param lastActionDtime 마지막 전이 일시(없으면 null)
 */
public record LocationConsentStatusRow(
      String userCd
    // 화면 표시는 "사용자명(사용자ID)" — 동명이인 구분을 위해 ID 를 함께 내린다.
    , String userId
    , String userNm
    , String userTypeCd
    , String nodeCd
    , String nodeNm
    , String consentState
    , String lastActionDtime
    , int purgeCount
) {
}
