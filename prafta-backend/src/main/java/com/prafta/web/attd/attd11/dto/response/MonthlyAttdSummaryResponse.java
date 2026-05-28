package com.prafta.web.attd.attd11.dto.response;

import java.util.List;

import com.prafta.web.attd.attd11.result.MonthlyAttdSummaryResult;

import lombok.Builder;
import lombok.Value;

/**
 * PRAFTA-034 - Attd_11 월별 사용자 근태 판정 응답.
 * 프론트는 monthlyAttdSummaryResultList 를 행으로 바인딩한다.
 */
@Value
@Builder
public class MonthlyAttdSummaryResponse {
    List<MonthlyAttdSummaryResult> monthlyAttdSummaryResultList;
}
