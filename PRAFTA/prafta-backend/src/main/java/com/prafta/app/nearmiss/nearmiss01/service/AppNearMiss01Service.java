package com.prafta.app.nearmiss.nearmiss01.service;

import com.prafta.app.nearmiss.nearmiss01.application.param.ChangeStatusParam;
import com.prafta.app.nearmiss.nearmiss01.application.param.IncidentDetailParam;
import com.prafta.app.nearmiss.nearmiss01.application.param.MyReportListParam;
import com.prafta.app.nearmiss.nearmiss01.application.param.ReportParam;
import com.prafta.app.nearmiss.nearmiss01.application.param.SiteIncidentListParam;
import com.prafta.app.nearmiss.nearmiss01.dto.response.IncidentInfoResponse;
import com.prafta.app.nearmiss.nearmiss01.dto.response.IncidentListResponse;
import com.prafta.app.nearmiss.nearmiss01.dto.response.ReportResponse;
import com.prafta.app.nearmiss.nearmiss01.dto.response.StatusCountResponse;

/**
 * 아차사고/사건 보고 (앱) 서비스.
 * A1 보고 등록 / A2 내 보고 목록 / A3 사업장 목록 / A4 상태카운트 / A5 상세 / A6 상태전환.
 */
public interface AppNearMiss01Service {

    // A1 근로자 아차사고 보고 등록 (multipart, 채번 + 사진 + 푸시 outbox)
    ReportResponse report(ReportParam param);

    // A2 내 보고 목록
    IncidentListResponse selectMyReports(MyReportListParam param);

    // A3 사업장 사건 목록 (관리자)
    IncidentListResponse selectSiteIncidents(SiteIncidentListParam param);

    // A4 상태별 카운트 (관리자)
    StatusCountResponse selectStatusCounts(SiteIncidentListParam param);

    // A5 사건 단건 상세 (본인 보고건 OR 사업장 관리자)
    IncidentInfoResponse selectIncidentInfo(IncidentDetailParam param);

    // A6 1차 확인 상태전환 (100->200 + 임시조치 / 900 반려)
    void changeStatus(ChangeStatusParam param);
}
