package com.prafta.app.siteops.admin.result;

/**
 * J1-7(prafta-app-025) 당일 열린(미퇴근) 일용직 출근행 조회 결과(퇴근 대상 ATTD_ID).
 */
public record SiteOpsOpenAttdResult(
    String attdId
    , String workYmd
) {
}
