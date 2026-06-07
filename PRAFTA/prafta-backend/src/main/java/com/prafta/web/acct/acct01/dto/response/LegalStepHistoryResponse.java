package com.prafta.web.acct.acct01.dto.response;

import java.util.List;

import com.prafta.web.acct.acct01.result.LegalStepHistoryResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LegalStepHistoryResponse {
    private List<LegalStepHistoryResult> historyList;
}
