package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사고 재해자 추가 요청. 순번은 서버가 MAX+1 채번한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AcctVictimAddRequest {
    private String siteCd;
    private String acctId;
    private AcctVictimItem victim;
}
