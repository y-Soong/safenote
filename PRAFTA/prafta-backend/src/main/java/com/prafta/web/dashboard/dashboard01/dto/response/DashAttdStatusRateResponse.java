package com.prafta.web.dashboard.dashboard01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 대시보드 근태 탭 A2 정상/비정상 근무율 응답 (PRAFTA-DASHBOARD-T2).
 * 합산 정합: normalCnt + lateCnt + earlyLeaveCnt + absentCnt == targetDayCnt.
 */
@Getter
@Builder
public class DashAttdStatusRateResponse {
    private int targetDayCnt;   // 판정 대상 계획일 수 (0 이면 FE "판정 대상 근무계획이 없습니다" 표시)
    private int normalCnt;      // 정상 일수
    private int lateCnt;        // 지각 일수
    private int earlyLeaveCnt;  // 조퇴 일수
    private int absentCnt;      // 미출근 일수
    private Double normalRate;  // 정상률 % (소수 1자리) — targetDayCnt=0 이면 null
}
