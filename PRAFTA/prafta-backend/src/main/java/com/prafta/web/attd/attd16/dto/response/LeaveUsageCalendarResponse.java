package com.prafta.web.attd.attd16.dto.response;

import java.util.List;

import com.prafta.web.attd.attd16.result.LeaveUsageCalendarRowResult;

import lombok.Builder;
import lombok.Value;

/**
 * ATTD16-T1 - 연차 사용 현황 캘린더 조회 응답.
 * 프론트(Attd_16.vue)는 leaveUsageCalendarResultList 를 dateYmd 로 그룹핑해 캘린더/상세를 만든다.
 * 조회 결과가 없으면 빈 배열(200).
 */
@Value
@Builder
public class LeaveUsageCalendarResponse {
    List<LeaveUsageCalendarRowResult> leaveUsageCalendarResultList;
}
