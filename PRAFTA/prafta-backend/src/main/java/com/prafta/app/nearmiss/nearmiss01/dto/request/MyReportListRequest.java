package com.prafta.app.nearmiss.nearmiss01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A2 내 보고 목록 조회 요청 (본인 보고건만).
 * cmpnyCd/userCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class MyReportListRequest {
    private String reportStatusCd; // 상태 필터(선택)
}
