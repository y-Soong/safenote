package com.prafta.app.attd.admin.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * J1-5: 월별 집계 응답.
 *
 * <p>직원별 근무일수/근무시간(분)/지각·조퇴 카운트(노드 스코프). PII 미포함 — 이름·노드명만.
 */
@Getter
@Builder
public class MonthlyAttdResponse {

    private final List<MonthlyItem> items;
    private final int totalCount;
    private final boolean hasMore;

    @Getter
    @Builder
    public static class MonthlyItem {
        private final String userCd;
        private final String userNm;
        private final String nodeNm;
        /** 근무일수(출근 기록 있는 distinct WORK_YMD, 차수 무관). */
        private final int workDays;
        /** 총 근무 분(휴게 제외, 음수 0). */
        private final long workMinutes;
        /** 지각 횟수(차수 단위). */
        private final int lateCnt;
        /** 조퇴 횟수(차수 단위). */
        private final int earlyCnt;
    }
}
