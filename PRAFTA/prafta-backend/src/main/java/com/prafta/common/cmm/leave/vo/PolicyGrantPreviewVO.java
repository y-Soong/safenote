package com.prafta.common.cmm.leave.vo;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 정책 기준 부여 프리뷰(dry-run) 집계 결과 (prafta-022 작업 D).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.7(권한).
 *
 * <p>"정책 기준 부여" 버튼 클릭 시, 실제 적용(부여) 전에 보여줄 집계 프리뷰다.
 * DB 쓰기 없이 조회만으로 직원별 추가예정 일수를 산정한다.
 *
 * <p>prafta-032 D6: 입사일 변경 처리방식 자동계산(KEEP 계열/RESET_ALL) 폐기로
 * "재발급(reissueCount)" 집계를 제거했다. 단일 동작 = 기존 부여 있으면 변경 없음,
 * 없으면 정책 기준 신규 부여.
 * <ul>
 *   <li>{@code selectedCount} : 프리뷰 대상으로 선택된 직원 수</li>
 *   <li>{@code newGrantCount} : 신규 부여 직원 수(addDays&gt;0)</li>
 *   <li>{@code noChangeCount} : 변경 없음 직원 수(addDays==0)</li>
 * </ul>
 */
@Getter
@Builder
public class PolicyGrantPreviewVO {

    /** 선택된 직원 수 */
    private int selectedCount;

    /** 신규 부여 직원 수(addDays>0) */
    private int newGrantCount;

    /** 변경 없음 직원 수(addDays==0) */
    private int noChangeCount;

    /** 직원별 프리뷰 행 */
    private List<PolicyGrantPreviewRowVO> rows;
}
