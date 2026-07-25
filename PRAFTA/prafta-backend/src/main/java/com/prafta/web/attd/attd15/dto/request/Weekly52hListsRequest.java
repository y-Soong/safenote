package com.prafta.web.attd.attd15.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ATTD15-T1 - 주52시간 관리 조회 요청.
 * 단일 주(weekStartYmd, 월요일 고정) 기준 사업장/(하위)부서/사용자명 필터.
 */
@Getter
@Setter
@NoArgsConstructor
public class Weekly52hListsRequest {
    private String siteCd;        // 사업장코드 (필수)
    private String nodeCd;        // 부서코드 (소속부서, nullable)
    private String incSubNodeYn;  // 하위부서 조회 여부 (Y/N)
    private String userNm;        // 사용자명 (부분일치, nullable)
    private String weekStartYmd;  // 대상 주 시작일(YYYYMMDD, 월요일 필수) — 필수
}
