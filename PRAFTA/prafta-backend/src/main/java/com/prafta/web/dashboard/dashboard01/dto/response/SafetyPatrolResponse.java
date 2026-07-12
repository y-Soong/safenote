package com.prafta.web.dashboard.dashboard01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 대시보드 안전 탭 순회점검 위젯(S2) 응답 (PRAFTA-DASHBOARD-T5).
 * 당일 카드(x/y)는 조회월과 무관하게 항상 오늘 기준 (사용자 확정 2026-07-07).
 */
@Getter
@Builder
public class SafetyPatrolResponse {
    private int todayInspectCnt; // 오늘 점검 기록이 있는 개소 수 (x)
    private int todayTotalCnt;   // 오늘 기준 사용중 개소 수 (y)
    private int monthMissCnt;    // 조회월 미이행 총 횟수 (개소별 미이행 일수 합. 미래월/당월 1일은 0)
}
