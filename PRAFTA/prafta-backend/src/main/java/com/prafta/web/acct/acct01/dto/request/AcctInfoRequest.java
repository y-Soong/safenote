package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사고 단건 상세 조회 요청 (사업장 스코프 강제).
 */
@Getter
@Setter
@NoArgsConstructor
public class AcctInfoRequest {
    private String siteCd;
    private String acctId;
}
