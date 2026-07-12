package com.prafta.web.dashboard.dashboard01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 대시보드 안전 탭 위젯 공용 조회 요청 (PRAFTA-DASHBOARD-T5).
 * S2 순회점검(/safety-patrol) · S3 위험성평가(/safety-risk) · S4 TBM 추이(/safety-tbm-trend) 3개 엔드포인트 공용.
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class DashSafetyRequest {
    private String siteCd; // 필수 — 상단 조회 사업장
    private String ym;     // 필수 — 조회월 'YYYY-MM'
}
