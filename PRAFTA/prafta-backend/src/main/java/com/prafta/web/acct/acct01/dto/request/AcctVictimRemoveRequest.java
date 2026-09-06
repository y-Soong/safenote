package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사고 재해자 제외 요청(물리 삭제). 마지막 1명은 서버가 차단한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AcctVictimRemoveRequest {
    private String siteCd;
    private String acctId;
    private Integer victimSeq;
}
