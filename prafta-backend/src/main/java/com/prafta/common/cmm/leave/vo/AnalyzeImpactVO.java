package com.prafta.common.cmm.leave.vo;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 정책 변경 영향 분석(화면 8) 전체 결과 묶음.
 *
 * <p>정책서: {@code .claude/requests/ref/prafta-017/CLAUDE_CODE_INSTRUCTIONS.md} §9.7 (응답 구조)
 *
 * <ul>
 *   <li>{@code summary}            — 요약 카드 4개</li>
 *   <li>{@code diff}               — axis별 변경 사항 (Baim_07 UI 순서로 정렬)</li>
 *   <li>{@code affectedEmployees}  — 영향받는 직원 목록 (expectedAdditional &gt; 0)</li>
 *   <li>{@code currentPolicySummary}/{@code targetPolicySummary} — 화면 상단 한 줄 요약(서버 생성)</li>
 * </ul>
 */
@Getter
@Builder
public class AnalyzeImpactVO {

    /** 요약 카드 4개 */
    private final AnalyzeImpactSummaryVO summary;

    /** axis별 변경 사항 (Baim_07 UI 순서) */
    private final List<ImpactDiffVO> diff;

    /** 영향받는 직원 목록 */
    private final List<AffectedEmployeeVO> affectedEmployees;

    /** 현재 정책 한 줄 요약 (예: "회계연도 기준 (비례 부여)") */
    private final String currentPolicySummary;

    /** 변경할 정책 한 줄 요약 */
    private final String targetPolicySummary;
}
