package com.prafta.web.attd.attd16.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ATTD16-T1 - 연차 사용 현황 캘린더 조회 요청.
 *
 * <p>월(searchYm) 단위로 사업장/(하위)부서 스코프의 연차 사용 실적을 조회한다.
 * 회사코드(cmpnyCd)는 파라미터로 받지 않는다 — 토큰 클레임만 신뢰(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class LeaveUsageCalendarRequest {
    private String siteCd;        // 사업장코드 (필수)
    private String nodeCd;        // 부서코드 (소속부서, 빈값이면 사업장 전체)
    private String incSubNodeYn;  // 하위부서 조회 여부 (Y/N, 기본 N — nodeCd 있을 때만 의미)
    private String searchYm;      // 조회 연월 (YYYYMM 6자리) — 필수
}
