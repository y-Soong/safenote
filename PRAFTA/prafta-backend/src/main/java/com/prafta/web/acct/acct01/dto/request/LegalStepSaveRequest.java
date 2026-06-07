package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ②탭 — 조치완료 체크/비고 저장 요청 (tb_acct_legal_step UPSERT).
 */
@Getter
@Setter
@NoArgsConstructor
public class LegalStepSaveRequest {
    private String siteCd;
    private String acctId;
    private String stepCd;
    private String isDoneYn; // Y/N
    private String remark;
}
