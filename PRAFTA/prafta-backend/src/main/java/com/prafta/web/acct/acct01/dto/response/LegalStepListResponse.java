package com.prafta.web.acct.acct01.dto.response;

import java.util.List;

import com.prafta.web.acct.acct01.result.LegalStepResult;

import lombok.Builder;
import lombok.Getter;

/**
 * ②탭 법정절차 응답. occurYmd 는 프론트 D-day 계산 기준(발생일).
 */
@Getter
@Builder
public class LegalStepListResponse {
    private String acctGradeCd;
    private String occurYmd;
    private List<LegalStepResult> legalStepList;
    private String notice;
}
