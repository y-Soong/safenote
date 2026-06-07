package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ②탭 — 등급별 법정절차 + 진행상태 조회 요청.
 */
@Getter
@Setter
@NoArgsConstructor
public class LegalStepListRequest {
    private String siteCd;
    private String acctId;
}
