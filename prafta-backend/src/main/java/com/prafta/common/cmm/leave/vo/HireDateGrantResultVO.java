package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 입사일 기준 연차 부여 결과(attd09 테스트/검증용).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5
 *
 * <ul>
 *   <li>{@code grantedUserCds} : 신규 부여가 1건 이상 발생한 직원</li>
 *   <li>{@code skippedUserCds} : 이미 올해 부여됨(멱등) 또는 부여 대상 아님(입사일 미래/0개월)으로 건너뜀</li>
 *   <li>{@code grantedDays}    : 이번 호출로 신규 부여된 총 일수</li>
 *   <li>{@code canceledCount}  : RESET_ALL 처리로 소프트 취소(CANCELED)된 기존 법정 부여 건수 (prafta-022 작업 C)</li>
 * </ul>
 */
@Getter
@Builder
public class HireDateGrantResultVO {

    private int grantedCount;
    private List<String> grantedUserCds;
    private int skippedCount;
    private List<String> skippedUserCds;
    private BigDecimal grantedDays;
    /** RESET_ALL 처리로 소프트 취소된 기존 법정 부여 총 건수 (prafta-022 작업 C). */
    private int canceledCount;
}
