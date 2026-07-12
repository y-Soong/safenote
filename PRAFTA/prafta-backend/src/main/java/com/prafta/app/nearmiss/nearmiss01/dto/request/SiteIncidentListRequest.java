package com.prafta.app.nearmiss.nearmiss01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A3 사업장 사건 목록 / A4 상태 카운트 조회 요청 (관리자, 사업장 스코프).
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단).
 * siteCd 는 JWT gv_siteCd 로 캐노니컬라이즈(본문값 무시).
 */
@Getter
@Setter
@NoArgsConstructor
public class SiteIncidentListRequest {
    private String reportStatusCd;
    private String potentialSeverityCd;
    private String startDate; // YYYY-MM-DD (발생일시 기준)
    private String endDate;   // YYYY-MM-DD
}
