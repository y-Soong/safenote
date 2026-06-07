package com.prafta.web.acct.acct01.dto.response;

import java.util.List;

import com.prafta.web.acct.acct01.result.RiskLinkResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RiskLinkResponse {
    private List<RiskLinkResult> riskList;
    private String notice;
}
