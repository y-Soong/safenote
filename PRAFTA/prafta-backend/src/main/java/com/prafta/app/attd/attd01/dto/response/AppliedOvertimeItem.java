package com.prafta.app.attd.attd01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-030 후속: 그날 적용(승인) 초과근무 1건 표시 항목.
 *
 * <p>출처 = TB_USER_OVERTIME_MGMT(적용 OT 실적, 취소 제외·구간 확정분만).
 *   응답은 일자별로 그룹되므로 workYmd 는 미포함(상위 days[]/카드 일자에 종속).
 *   구간은 실제 시작/종료(ACTUAL_*) 일자·시각으로 표현 — 오버나이트(날짜 넘김)는 startDate/endDate 가
 *   각각 일자를 보유하므로 (일자+시각) 결합으로 구분된다.
 *
 * <p>⚠️ boolean is-접두 필드 없음(Lombok+Jackson is-탈락 함정 비대상). 직렬화 키는 필드명 그대로.
 */
@Getter
@Builder
public class AppliedOvertimeItem {
    private final String startDate;     // ACTUAL_START_DATE YYYYMMDD
    private final String startTime;     // ACTUAL_START_TIME HHmm
    private final String endDate;       // ACTUAL_END_DATE YYYYMMDD
    private final String endTime;       // ACTUAL_END_TIME HHmm
    private final Integer workMinutes;  // WORK_MINUTES (nullable)
}
