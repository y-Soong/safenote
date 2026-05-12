package com.prafta.web.attd.attd07.dto.response;

import java.util.List;

import com.prafta.web.attd.attd07.result.MonthlyAttdListResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqSummaryResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AttdRecordListResponse {
    List<MonthlyAttdListResult> attdRecordResultList;
    List<MonthlyAttdReqSummaryResult> monthlyAttdReqSummaryResultList;
}
