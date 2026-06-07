package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사고 수정 요청 (등급/경위/처리상태/장소 등 변경).
 * 등급 변경 시 법정 기한 재계산은 프론트가 수행(본 요청은 값 저장만).
 */
@Getter
@Setter
@NoArgsConstructor
public class AcctUpdateRequest {
    private String siteCd;
    private String acctId;
    private String occurYmd;          // YYYYMMDD
    private String occurTime;         // HHMM
    private String occurPlace;
    private String acctGradeCd;   // SYS065
    private String acctDesc;
    private String employerDesc;
    private String processStatusCd;   // SYS066
}
