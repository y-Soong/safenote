package com.prafta.web.nearmiss.nearmiss01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E1 사건 목록 조회 요청 (상태탭/유형/잠재중대성/기간 필터).
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class IncidentListRequest {
    private String siteCd;
    private String reportStatusCd;
    private String incidentTypeCd;
    private String potentialSeverityCd;
    private String startDate; // YYYY-MM-DD (발생일시 기준)
    private String endDate;   // YYYY-MM-DD
}
