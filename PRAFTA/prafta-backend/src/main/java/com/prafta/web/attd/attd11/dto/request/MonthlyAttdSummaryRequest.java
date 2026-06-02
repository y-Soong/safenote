package com.prafta.web.attd.attd11.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PRAFTA-034 - Attd_11 월별 사용자 근태 판정 조회 요청.
 * 단일 월(workYm) 기준 사업장/(하위)부서/사용자명 필터.
 */
@Getter
@Setter
@NoArgsConstructor
public class MonthlyAttdSummaryRequest {
    private String workYm;        // "2026-05" 또는 "202605" (단일 월)
    private String siteCd;        // 사업장코드
    private String nodeCd;        // 부서코드 (소속부서)
    private String incSubNodeYn;  // 하위부서 조회 여부 (Y/N)
    private String userNm;        // 사용자명 (부분일치, nullable)
}
