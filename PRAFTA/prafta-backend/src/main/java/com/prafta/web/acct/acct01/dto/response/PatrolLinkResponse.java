package com.prafta.web.acct.acct01.dto.response;

import java.util.List;

import com.prafta.web.acct.acct01.result.PatrolItemResult;
import com.prafta.web.acct.acct01.result.PatrolLinkResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PatrolLinkResponse {
    private List<PatrolLinkResult> summaryList;
    private List<PatrolItemResult> badItemList;
    private String notice;
}
