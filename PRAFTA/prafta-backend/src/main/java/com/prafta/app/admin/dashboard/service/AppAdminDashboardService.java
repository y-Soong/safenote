package com.prafta.app.admin.dashboard.service;

import com.prafta.app.admin.dashboard.application.param.DashboardSummaryParam;
import com.prafta.app.admin.dashboard.dto.response.DashboardSummaryResponse;

/**
 * J1-10 (B-5): 관리자 대시보드 요약 서비스(조회 전용).
 */
public interface AppAdminDashboardService {

    /**
     * 대시보드 요약(4영역 카운트 + 위젯별 available) 조회.
     * <p>진입 1차 게이트(2-1) 통과 시 200, 각 위젯의 권한은 available 로 표현한다.
     */
    DashboardSummaryResponse selectSummary(DashboardSummaryParam param);
}
