package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사고 등록 요청.
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단). siteCd 는 발생 사업장(스코프 검증).
 */
@Getter
@Setter
@NoArgsConstructor
public class AcctCreateRequest {
    private String siteCd;
    private String victimUserTypeCd; // SYS050 REGULAR/DAILY
    private String victimUserCd;
    private String occurYmd;          // YYYYMMDD
    private String occurTime;         // HHMM
    private String occurPlace;
    private String acctGradeCd;   // SYS065 100/200/300
    private String acctDesc;      // 사고 경위
    private String employerDesc;      // 신고의무자(직접입력)
}
