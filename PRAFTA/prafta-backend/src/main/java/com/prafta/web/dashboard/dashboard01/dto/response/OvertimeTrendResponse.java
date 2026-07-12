package com.prafta.web.dashboard.dashboard01.dto.response;

import java.util.List;

import com.prafta.web.dashboard.dashboard01.result.OvertimeMonthlyResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 대시보드 근태 탭 A3 초과근무 6개월 추이 위젯 응답 (PRAFTA-DASHBOARD-T3).
 * monthlyList 는 항상 길이 6 (빈 월 totalMinutes=0, service 채움), 과거→조회월 오름차순
 * [{ ym: 'YYYY-MM', totalMinutes }].
 */
@Getter
@Builder
public class OvertimeTrendResponse {
    private List<OvertimeMonthlyResult> monthlyList; // [{ ym: 'YYYY-MM', totalMinutes }] × 6
}
