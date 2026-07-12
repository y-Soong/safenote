package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 위험성평가 개선실행계획서/개선완료보고서 출력(②) 보강 요청.
 * acctId 로 사고 헤더(사업장 스코프)를 서버에서 도출하고, assessmentCd 는
 * 해당 사고의 RISK 연계(TB_ACCT_LINK)에 실제 등록된 값인지 정확 매칭으로 재검증한다(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class RiskAssessmentPrintRequest {
    private String siteCd;
    private String acctId;
    private String assessmentCd;
}
