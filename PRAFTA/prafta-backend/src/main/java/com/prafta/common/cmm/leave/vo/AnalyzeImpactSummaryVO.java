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

    /**
     * "분석 결과 없음" 사유 구분 (T4-06 / 3.4). FE가 사유별 안내 문구를 띄울 수 있게 백엔드가 명시.
     *
     * <ul>
     *   <li>{@code null}            — 영향받는 직원이 있음(정상, affectedCount &gt; 0). 별도 안내 불필요</li>
     *   <li>{@code NO_TARGET}       — 대상 직원 없음 (입사일 미입력/비활성으로 전원 제외, totalEmployees=0)</li>
     *   <li>{@code NO_ADDITIONAL}   — 대상은 있으나 추가 부여 없음 (정책 변경이 일수를 늘리지 않음)</li>
     * </ul>
     *
     * <p>① "변경 사항 없음"은 본 분석 진입 전 {@code ATTD_400_021}(400) 으로 차단되므로 여기에는 오지 않는다.
     */
    private final String noResultReason;
}
