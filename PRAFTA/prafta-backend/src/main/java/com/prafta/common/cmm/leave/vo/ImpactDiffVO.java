package com.prafta.common.cmm.leave.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * 정책 변경 영향 분석(화면 8)의 axis별 변경 사항(diff) 1행.
 *
 * <p>정책서: {@code .claude/requests/ref/prafta-017/CLAUDE_CODE_INSTRUCTIONS.md} §9.5 (Diff 패널 표시 규칙)
 *
 * <p>표시 순서는 Baim_07 UI 7-axis 순서(1 부여기준 / 2 입사첫해[axis3] / 3 반올림[axis4]
 * / 4 회계연도시작일[axis2] / 5 근속 / 6 유효 / 7 사용촉진)를 따른다(가드레일 2).
 * fromValue/toValue/note는 코드값을 한글 라벨로 변환한 표시용 문자열이다(프론트는 표시만).
 *
 * @see #changeType — CHANGED / DEACTIVATED / ACTIVATED / UNCHANGED (§9.5.1)
 */
@Getter
@Builder
public class ImpactDiffVO {

    /** 백엔드 axis 번호 (1~7). 표시 순서와 별개. */
    private final int axisNum;

    /** axis 라벨 (예: "연차 부여 기준") */
    private final String axisName;

    /** 변경 전 값(표시용 한글 라벨, UNCHANGED/ACTIVATED 시 null 가능) */
    private final String fromValue;

    /** 변경 후 값(표시용 한글 라벨, DEACTIVATED 시 "(비활성)" 등) */
    private final String toValue;

    /** 변경 유형: CHANGED / DEACTIVATED / ACTIVATED / UNCHANGED */
    private final String changeType;

    /** 부가 설명(예: "법정 기준 유지 (n=3, m=2, max=25)"). UNCHANGED 표시에 활용. */
    private final String note;
}
