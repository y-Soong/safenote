package com.prafta.common.cmm.leave.vo;

import java.util.List;

/**
 * PC-07(D9-③): 소멸 임박 짜투리 리포트 결과 (OFF 회사 지원 — Attd_09 리포트 섹션).
 *
 * @param remnantPolicyOn 짜투리 보전 옵션 ON 여부(FE 섹션 분기)
 * @param rows            0 &lt; 잔여 &lt; 본인 최소단위 요금인 사용자 목록(최근접 소멸일 오름차순)
 */
public record RemnantReportVO(
      boolean remnantPolicyOn
    , List<RemnantReportRowVO> rows
) {
}
