package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 근태 + TBM 합본 출력(③) 집계 요청.
 * acctId 로 사고 헤더(발생일/재해자/사업장)를 서버에서 도출하므로 victim/occurYmd 는 클라가 넘기지 않는다(IDOR 차단).
 * siteCd 는 1차 접근 검증용이며, 실제 조회 스코프는 사고 헤더 siteCd 로 재검증한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AttdTbmPrintRequest {
    private String siteCd;
    private String acctId;
}
