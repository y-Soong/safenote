package com.prafta.web.acct.acct01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사고 등록 요청.
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단). siteCd 는 발생 사업장(스코프 검증).
 * 재해자는 victimList 배열(1~50명)로 받는다(prafta-065). 대표 재해자 = 배열 첫 인원(서버가 헤더 컬럼에 반영).
 */
@Getter
@Setter
@NoArgsConstructor
public class AcctCreateRequest {
    private String siteCd;
    private List<AcctVictimItem> victimList;
    private String occurYmd;          // YYYYMMDD
    private String occurTime;         // HHMM
    private String occurPlace;
    private String acctGradeCd;   // SYS065 100/200/300
    private String acctDesc;      // 사고 경위
    private String employerDesc;      // 신고의무자(직접입력)
}
