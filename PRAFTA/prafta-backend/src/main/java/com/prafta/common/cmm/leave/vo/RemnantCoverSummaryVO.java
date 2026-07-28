package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * PC-07(D9-②): 회사 부담 보전 연간 집계 결과 (Attd_09 상단 칩 + 상세 목록).
 *
 * @param remnantPolicyOn 짜투리 보전 옵션 ON 여부(FE 섹션 분기 — ON: 집계 칩 / OFF: 소멸 임박 리포트)
 * @param year            집계 연도(YYYY)
 * @param totalCoverDays  회사 부담 합계(일) — COVER_DAYS &gt; 0 행만(전액 회수분 제외)
 * @param coverCount      부담 건수(M건)
 * @param items           상세 목록(COVER 행 + 사용자명)
 */
public record RemnantCoverSummaryVO(
      boolean remnantPolicyOn
    , String year
    , BigDecimal totalCoverDays
    , int coverCount
    , List<RemnantCoverListRowVO> items
) {
}
