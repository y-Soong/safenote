package com.prafta.web.acct.acct01.dto.response;

import java.util.List;

import com.prafta.web.acct.acct01.result.NearMissLinkResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NearMissLinkResponse {
    private List<NearMissLinkResult> nearMissList;
    private String notice;
}
