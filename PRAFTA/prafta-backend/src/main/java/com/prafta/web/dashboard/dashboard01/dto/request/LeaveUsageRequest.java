package com.prafta.web.dashboard.dashboard01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 대시보드 근태 탭 A4 법정연차 사용/사용예정/미사용 3분할 조회 요청 (PRAFTA-DASHBOARD-T3).
 * 현재 시점 스냅샷 — baseYm(조회월) 미수신 (§1 A4 확정). cmpnyCd 는 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class LeaveUsageRequest {
    private String siteCd;       // 필수 — 상단 조회 사업장
    private String nodeCd;       // 선택 — 부서코드 (공란 = 사업장 전체)
    private String incSubNodeYn; // 선택 — 하위부서 포함 Y/N (기본 N)
}
