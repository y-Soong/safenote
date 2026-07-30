package com.prafta.web.attd.attd16.service;

import com.prafta.web.attd.attd16.application.param.LeaveUsageCalendarParam;
import com.prafta.web.attd.attd16.dto.response.LeaveUsageCalendarResponse;

/**
 * ATTD16-T1 - 연차 사용 현황 캘린더 서비스(읽기 전용).
 */
public interface Attd16Service {

    /** 월별 연차 사용 현황(일자 전개) 조회. */
    LeaveUsageCalendarResponse getLeaveUsageCalendar(LeaveUsageCalendarParam param);
}
