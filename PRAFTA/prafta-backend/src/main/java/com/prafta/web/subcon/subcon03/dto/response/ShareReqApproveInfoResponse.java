package com.prafta.web.subcon.subcon03.dto.response;

import java.util.List;

import com.prafta.web.subcon.subcon03.result.RelayCandidateResult;

import lombok.Builder;
import lombok.Value;

/**
 * 승인 사전정보 응답(PRAFTA-SUBCON-T3 §5-4·§5-7) — 제공측 승인 팝업 전용.
 *
 * <p>closedAll=false 이고 closedOnlyYn='Y' 면 승인 불가(서버도 SUBCON_409_007 로 차단).
 * relayCandidates 에는 하위 제공사 회사코드/회사명이 포함되지 않는다.
 */
@Value
@Builder
public class ShareReqApproveInfoResponse {
    Long shareReqId;
    String reqCmpnyNm;          // 요청 회사명(직상위 상대 — 인명은 미포함)
    String siteNm;              // 대상 사업장명(제공측 자기 테넌트 사업장)
    String dataType;
    String periodStr;
    String periodEnd;
    String periodLabel;         // "YYYY-MM-DD ~ YYYY-MM-DD"
    String closedOnlyYn;
    String purpose;
    boolean closedAll;          // 요청 기간 전 월 마감 커버 여부
    List<String> unclosedYms;   // 미마감 월(YYYY-MM)
    List<RelayCandidateResult> relayCandidates;
}
