package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사고 재해자 속성 수정 요청. 인물(유형·코드)은 바꾸지 않는다(바꾸려면 제외 후 추가).
 */
@Getter
@Setter
@NoArgsConstructor
public class AcctVictimUpdateRequest {
    private String siteCd;
    private String acctId;
    private Integer victimSeq;
    private String victimResultCd;
    private Integer careDays;
    private Integer restDays;
    private String injuryPart;
    private String injuryDesc;
}
