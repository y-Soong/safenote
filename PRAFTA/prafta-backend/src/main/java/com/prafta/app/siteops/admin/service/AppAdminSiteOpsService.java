package com.prafta.app.siteops.admin.service;

import com.prafta.app.siteops.admin.application.param.SiteOpsQrParam;
import com.prafta.app.siteops.admin.dto.response.SiteOpsAttendanceResponse;

/**
 * J1-7(prafta-app-025) 관리자 현장 일용직 QR 출퇴근 등록 서비스.
 *
 * <p>C1: 진입 게이트(SITE_OPS)·사업장 스코프·대상 유효성·멱등은 모두 서버가 최종 판정한다.
 */
public interface AppAdminSiteOpsService {

    /** S1 일용직 QR 출근 등록(멱등). */
    SiteOpsAttendanceResponse checkIn(SiteOpsQrParam param);

    /** S2 일용직 QR 퇴근 등록(멱등). */
    SiteOpsAttendanceResponse checkOut(SiteOpsQrParam param);
}
