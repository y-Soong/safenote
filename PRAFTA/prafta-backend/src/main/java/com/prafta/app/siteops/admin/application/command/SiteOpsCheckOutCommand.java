package com.prafta.app.siteops.admin.application.command;

/**
 * J1-7(prafta-app-025) 일용직 퇴근(check-out) UPDATE 커맨드(TB_USER_ATTD_MGMT).
 *
 * <p>대상 ATTD_ID 의 CHECK_OUT_* 를 채운다. 동시성 가드: WHERE CHECK_OUT_TIME IS NULL(이미 퇴근됐으면 0건).
 * CHECK_OUT_METHOD=관리자 QR 현장 등록 코드. UPDATE_NO=관리자 USER_CD.
 */
public record SiteOpsCheckOutCommand(
    String attdId
    , String cmpnyCd
    , String userCd
    , String checkOutDate
    , String checkOutTime
    , String checkOutMethod
) {
}
