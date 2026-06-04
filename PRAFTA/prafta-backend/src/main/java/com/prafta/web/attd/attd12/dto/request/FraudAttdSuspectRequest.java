package com.prafta.web.attd.attd12.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-com-003 C6 - 부정 출퇴근 의심 조회 요청.
 * 단일 월(workYm) 기준 사업장/(하위)부서 스코프. Attd_11 입력 패턴 차용.
 */
@Getter
@Setter
@NoArgsConstructor
public class FraudAttdSuspectRequest {
    private String workYm;        // "2026-06" 또는 "202606" (단일 월)
    private String siteCd;        // 사업장코드
    private String nodeCd;        // 부서코드 (소속부서)
    private String incSubNodeYn;  // 하위부서 조회 여부 (Y/N)
    private String suspectType;   // 의심유형 필터 ("" 전체 / RULE1 / RULE2 / RULE3)
}
