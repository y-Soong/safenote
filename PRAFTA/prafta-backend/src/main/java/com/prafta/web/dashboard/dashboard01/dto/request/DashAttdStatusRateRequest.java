package com.prafta.web.dashboard.dashboard01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 대시보드 근태 탭 A2 정상/비정상 근무율 조회 요청 (PRAFTA-DASHBOARD-T2).
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class DashAttdStatusRateRequest {
    private String siteCd;       // 필수 — 상단 조회 사업장
    private String nodeCd;       // 선택 — 부서코드 (공란 = 사업장 전체, 루트 확장)
    private String incSubNodeYn; // 선택 — 하위부서 포함 Y/N (기본 N)
    private String workYm;       // 필수 — 조회월 'YYYY-MM'
}
