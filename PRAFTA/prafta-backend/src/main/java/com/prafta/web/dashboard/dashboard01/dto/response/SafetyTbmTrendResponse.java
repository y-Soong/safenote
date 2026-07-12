package com.prafta.web.dashboard.dashboard01.dto.response;

import java.util.List;

import com.prafta.web.dashboard.dashboard01.result.TbmMonthCntResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 대시보드 안전 탭 TBM 12개월 추이 위젯(S4) 응답 (PRAFTA-DASHBOARD-T5).
 * trend 는 항상 길이 12 (빈 월 cnt=0, service 채움), 과거→조회월 오름차순.
 */
@Getter
@Builder
public class SafetyTbmTrendResponse {
    private List<TbmMonthCntResult> trend; // [{ ym: 'YYYY-MM', cnt }] × 12
}
