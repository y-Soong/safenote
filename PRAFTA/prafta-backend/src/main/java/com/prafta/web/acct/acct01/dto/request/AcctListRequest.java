package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사고 목록 조회 요청 (사업장/등급/처리상태/기간 필터).
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class AcctListRequest {
    private String siteCd;
    private String acctGradeCd;
    private String processStatusCd;
    private String startDate; // YYYY-MM-DD (발생일 기준)
    private String endDate;   // YYYY-MM-DD
    private String searchKeyword;
}
