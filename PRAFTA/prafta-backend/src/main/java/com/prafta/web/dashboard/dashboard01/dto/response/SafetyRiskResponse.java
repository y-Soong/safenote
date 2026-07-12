package com.prafta.web.dashboard.dashboard01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 대시보드 안전 탭 위험성평가 위젯(S3) 응답 (PRAFTA-DASHBOARD-T5).
 * 상태 카운트 2종은 월 조건 없음(Risk_03 목록 건수 일치), 아차사고는 발생일시 기준 조회월 필터.
 */
@Getter
@Builder
public class SafetyRiskResponse {
    private int reviewRequestCnt; // 검토요청(SYS011=001) 건수 — 월 무관
    private int improvePlanCnt;   // 개선예정(SYS011=002) 건수 — 월 무관
    private int nearMissCnt;      // 조회월 아차사고 등록 건수 (OCCUR_DTIME 기준)
}
