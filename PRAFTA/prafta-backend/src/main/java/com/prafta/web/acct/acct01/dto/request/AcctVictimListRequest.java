package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사고 재해자 목록 조회 요청 (사업장 스코프 강제).
 */
@Getter
@Setter
@NoArgsConstructor
public class AcctVictimListRequest {
    private String siteCd;
    private String acctId;
}
