package com.prafta.web.subcon.subcon03.dto.response;

import java.util.List;

import com.prafta.web.subcon.subcon03.result.CoverageMonthResult;
import com.prafta.web.subcon.subcon03.result.RelayCandidateResult;

import lombok.Builder;
import lombok.Value;

/**
 * 승인 사전정보 응답(PRAFTA-SUBCON-T3 §5-4·§5-7) — 제공측 승인 팝업 전용.
 *
 * <p>[D-1/D-2, 2026-07-22] "마감분만" 은 더 이상 승인 차단 게이트가 아니다(SUBCON_409_007 미사용
 * 전환). closedAll/unclosedYms 는 하위호환을 위해 유지하되, 실제 포함/제외 범위 예고는
 * {@code coverageMonths}/{@code includedRowCnt}/{@code expectedEmptyYn}(PS-06) 을 본다.
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
    boolean closedAll;          // 요청 기간 전 월 마감 커버 여부(하위호환 — PS-01 반영값)
    List<String> unclosedYms;   // 미마감 월(YYYY-MM, 하위호환)
    List<RelayCandidateResult> relayCandidates;

    /**
     * [PS-06] 월별 마감 커버리지 예고(D-1/D-2) — closedOnlyYn='Y' && dataType=ATTD 일 때만 산출.
     * ym 은 이 응답에서 "YYYY-MM" 으로 변환해 내려준다(META 저장값은 YYYYMM). 그 외(RISK/NEARMISS,
     * closedOnlyYn='N')는 null(기존 응답 무변경).
     */
    List<CoverageMonthResult> coverageMonths;

    /**
     * [PS-06] 커버리지 필터 통과 예상 행수 — closedOnlyYn='Y' 일 때만 산출(동의 필터는 미적용이라
     * 실제 승인 시 미동의 제외로 더 줄 수 있음 — 팝업 문구 안내). 'N' 이면 null(전량 — 기존 UI 유지).
     */
    Integer includedRowCnt;

    /** [PS-06, D-1] includedRowCnt==0 이면 'Y'(0건 경고 트리거). 산출 대상이 아니면 null. */
    String expectedEmptyYn;
}
