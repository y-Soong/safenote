package com.prafta.web.attd.attd09.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.prafta.common.cmm.leave.vo.RemnantCoverListRowVO;

import lombok.Builder;
import lombok.Value;

/**
 * PC-07(D9-②): 회사 부담 보전 연간 집계 응답.
 * GET /attd09/leave-dashboard/remnant-cover-summary.
 *
 * <p>FE(Attd_09 — UI-B): 짜투리 보전 ON 회사만 집계 칩 노출({@code remnantPolicyOn} 분기).
 */
@Value
@Builder
public class RemnantCoverSummaryResponse {

    /** 짜투리 보전 옵션 ON 여부 — FE 섹션 분기(ON: 집계 칩 / OFF: 소멸 임박 리포트). */
    boolean remnantPolicyOn;

    /** 집계 연도(YYYY). */
    String year;

    /** 회사 부담 합계(일) — COVER_DAYS &gt; 0 행만(전액 회수분 제외). */
    BigDecimal totalCoverDays;

    /** 부담 건수(M건). */
    int coverCount;

    /** 상세 목록(COVER 행 + 사용자명 — 칩 클릭 상세용). */
    List<RemnantCoverListRowVO> items;
}
