package com.prafta.web.acct.acct01.dto.response;

import com.prafta.web.acct.acct01.result.AcctResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AcctInfoResponse {
    private AcctResult acctInfo;
}
