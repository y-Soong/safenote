package com.prafta.web.attd.attd11.service;

import com.prafta.web.attd.attd11.application.param.MonthlyAttdSummaryParam;
import com.prafta.web.attd.attd11.dto.response.MonthlyAttdSummaryResponse;

/**
 * PRAFTA-034 - Attd_11 월별 사용자 근태 판정 서비스.
 */
public interface Attd11Service {

    /**
     * 단일 월·사업장·(하위)부서·사용자명 기준 사용자 1명당 1행 월간 근태 종합을 산출한다.
     * (근무일수/총 근무시간/초과근무/지각·조퇴 횟수·시간) decisions §2/§4.
     */
    MonthlyAttdSummaryResponse getMonthlyAttdSummary(MonthlyAttdSummaryParam param);
}
