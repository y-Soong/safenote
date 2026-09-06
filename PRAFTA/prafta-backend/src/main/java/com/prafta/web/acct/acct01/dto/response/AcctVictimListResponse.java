package com.prafta.web.acct.acct01.dto.response;

import java.util.List;

import com.prafta.web.acct.acct01.result.AcctVictimResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AcctVictimListResponse {
    private List<AcctVictimResult> victimList;
}
