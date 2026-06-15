package com.prafta.web.risk.risklink01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * L2 특정 위험성평가 건에 연결된(USE_YN='Y') 아차사고 목록 조회 요청.
 * cmpnyCd 는 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class LinkedNearMissRequest {
    private String siteCd;
    private String processCd;
    private String assessmentCd;
}
