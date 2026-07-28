package com.prafta.web.attd.attd07.dto.response;

import java.util.List;

import com.prafta.web.attd.attd07.result.ConfirmedLeaveResult;
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
    /** PRAFTA-APP-018-F: 그날 확정 연차 사용내역(자동확정/직접 포함, 미처리 결재대기 제외). 표시 전용. */
    List<ConfirmedLeaveResult> confirmedLeaveResultList;

    /**
     * PC-07(N8): 대상 사용자·대상일 기준 1일 환산시간(분, 개인 분모 480 캡 — 미산출 480 폴백).
     * AttdDayDetailPop 의 연차 "N일 H시간 M분" 표기 분모(기존 480 고정 폴백 결함 D2 해소, additive).
     */
    int convMinutes;
}
