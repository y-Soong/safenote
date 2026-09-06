package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 재해자 1명 항목(등록 배열 원소 / 재해자 추가 body 의 victim).
 * userTypeCd 는 SYS050 REGULAR/DAILY, victimResultCd 는 SYS084(DEATH/INJURY/DISEASE).
 * careDays/restDays 는 등록 시 미확정 가능(null 허용, 0~3650).
 */
@Getter
@Setter
@NoArgsConstructor
public class AcctVictimItem {
    private String userTypeCd;
    private String userCd;
    private String victimResultCd;
    private Integer careDays;
    private Integer restDays;
    private String injuryPart;
    private String injuryDesc;
}
