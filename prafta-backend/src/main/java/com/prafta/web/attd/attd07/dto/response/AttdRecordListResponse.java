package com.prafta.web.attd.attd07.dto.response;

import java.util.List;

import com.prafta.web.attd.attd07.result.MonthlyAttdListResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqSummaryResult;
import com.prafta.web.attd.attd07.result.MonthlyOvertimeResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AttdRecordListResponse {
    List<MonthlyAttdListResult> attdRecordResultList;
    List<MonthlyAttdReqSummaryResult> monthlyAttdReqSummaryResultList;

    /* PRAFTA-017 - 일자별 초과근무 목록 (월 단위). selectMonthlyOvertimeList 결과. */
    List<MonthlyOvertimeResult> monthlyOvertimeResultList;
}
