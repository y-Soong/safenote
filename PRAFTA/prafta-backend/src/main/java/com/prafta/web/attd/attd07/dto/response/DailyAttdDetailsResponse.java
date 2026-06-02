package com.prafta.web.attd.attd07.dto.response;

import java.util.List;

import com.prafta.web.attd.attd07.result.DailyAttdDetailHistoryResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailsResult;
import com.prafta.web.attd.attd07.result.DailyOvertimeResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DailyAttdDetailsResponse {
    DailyAttdDetailsResult dailyAttdDetailsResult;
    List<DailyAttdDetailHistoryResult> dailyAttdDetailHistoryResultList;
    /** PRAFTA-003-7: 응답 키 명명 컨벤션(lowerCamel)에 맞춰 `monthlyAttdReqResultList`로 정규화. */
    List<MonthlyAttdReqResult> monthlyAttdReqResultList;
    /** PRAFTA-003-6: 해당 일자에 등록된 초과근무(OT) 목록. 응답 키는 lowerCamel(`dailyOvertimeResultList`). */
    List<DailyOvertimeResult> dailyOvertimeResultList;
}
