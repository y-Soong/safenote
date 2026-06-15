package com.prafta.app.siteops.admin.application.command;

/**
 * J1-7(prafta-app-025) 일용직 출근(check-in) INSERT 커맨드(TB_USER_ATTD_MGMT).
 *
 * <p>일용직 전용 단순화: NODE_CD=NULL(부서 없음), WORK_SEQ=1 고정, DEVICE_UUID=NULL(관리자 단말이므로
 * 일용직 단말 식별 의미 없음), GPS 미저장. CHECK_IN_METHOD=관리자 QR 현장 등록 코드(self '01'과 구분).
 * INSERT_NO=관리자 USER_CD.
 */
public record SiteOpsCheckInCommand(
    String attdId
    , String cmpnyCd
    , String siteCd
    , String userCd
    , String workYmd
    , int workSeq
    , String checkInDate
    , String checkInTime
    , String checkInMethod
    , String insertNo
) {
}
