package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * 정책 변경 영향 분석(화면 8) 상단 요약 카드 4개 값.
 *
 * <p>정책서: {@code .claude/requests/ref/prafta-017/CLAUDE_CODE_INSTRUCTIONS.md} §9.2-3 (요약 카드)
 *
 * <p>기존 {@link ImpactSummaryVO}(HISTORY.IMPACT_SUMMARY 보존용)와 별도로, 화면 8 전용
 * 4-카드 요약을 운반한다. {@code additionalDaysTotal}은 1년치 부여 시뮬레이션 기반 근사치다.
 */
@Getter
@Builder
public class AnalyzeImpactSummaryVO {

    /** 전체 활성 직원 수 */
    private final int totalEmployees;

    /** 정상 적용(영향 없는) 인원 수 */
    private final int normalCount;

    /** 주의 필요(영향받는) 인원 수 */
    private final int affectedCount;

    /** 추가 부담 합계 일수 (Σ expectedAdditional, 근사) */
    private final BigDecimal additionalDaysTotal;
}
