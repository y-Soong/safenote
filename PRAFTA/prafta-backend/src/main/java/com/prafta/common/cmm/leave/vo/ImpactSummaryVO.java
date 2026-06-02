package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 정책 변경 영향 분석 결과.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.2 (TB_LEAVE_POLICY_HISTORY.IMPACT_SUMMARY)
 *
 * <p>본 VO는 두 가지 용도로 사용된다:
 * <ul>
 *   <li>화면 미리보기(impact-preview endpoint) - JSON 응답 그대로</li>
 *   <li>정책 변경 시 TB_LEAVE_POLICY_HISTORY.IMPACT_SUMMARY 컬럼에 직렬화하여 보존</li>
 * </ul>
 *
 * <p>정책서 §8.5에 정확한 계산 공식이 명시되어 있지 않은 항목은 단순 근사로 산출한다.
 * - {@code affectedUserCount}: 본 정책에 영향을 받을 수 있는 활성 사용자 수 (CMPNY_CD 격리)
 * - {@code estimatedAdditionalDays}: 변경 전후 정책에 따른 추가 부여 일수 추정.
 *   단순 근사이므로 실제 일배치 부여 결과와 차이가 있을 수 있다.
 */
@Getter
@Builder
public class ImpactSummaryVO {

    /** 영향받는 활성 사용자 수 */
    private final int affectedUserCount;

    /** 예상 추가 부여 일수 합계 (근사) */
    private final BigDecimal estimatedAdditionalDays;

    /** 변경된 axis 컬럼명 목록 (예: ["AXIS5_MAX_DAYS","AXIS7_USE_PROMOTION"]) */
    private final List<String> axesChanged;

    /** 미리보기 수행 시각 (ISO 8601) */
    private final String previewedAt;
}
