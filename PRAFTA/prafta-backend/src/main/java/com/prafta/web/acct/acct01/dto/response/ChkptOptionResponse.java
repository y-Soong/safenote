package com.prafta.web.acct.acct01.dto.response;

import java.util.List;

import com.prafta.web.acct.acct01.result.ChkptOptionResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChkptOptionResponse {
    private List<ChkptOptionResult> chkptOptionList;
}
