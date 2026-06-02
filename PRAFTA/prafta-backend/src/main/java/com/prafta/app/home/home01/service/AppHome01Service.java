package com.prafta.app.home.home01.service;

import com.prafta.app.home.home01.application.param.HomeSummaryParam;
import com.prafta.app.home.home01.dto.response.HomeSummaryResponse;

/**
 * prafta-app-001: 앱 메인화면 요약 서비스 인터페이스.
 */
public interface AppHome01Service {

    /**
     * 메인화면 요약(attendance / leave / approval / tbm) 단일 조회.
     */
    HomeSummaryResponse selectHomeSummary(HomeSummaryParam param);
}
