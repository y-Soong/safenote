package com.prafta.web.dashboard.dashboard01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 대시보드 안전 탭 무사고 배너(S1) + 사고 summary(S5) 조회 요청 (PRAFTA-DASHBOARD-T4).
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class DashSafetyAcctRequest {
    private String siteCd; // 필수 — 상단 조회 사업장
    private String ym;     // 필수 — 조회월 'YYYY-MM' (summary 등급 카운트 범위)
}
