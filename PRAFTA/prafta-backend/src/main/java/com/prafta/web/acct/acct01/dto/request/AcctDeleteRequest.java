package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사고 soft delete 요청 (DEL_YN='Y').
 */
@Getter
@Setter
@NoArgsConstructor
public class AcctDeleteRequest {
    private String siteCd;
    private String acctId;
}
