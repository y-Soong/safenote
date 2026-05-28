package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 연차 현황 대시보드(attd09) 상단 메트릭 카드 4종의 매퍼 결과.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5
 *
 * <ul>
 *   <li>{@code totalEmployees}     — 활성 직원 수 (USE_YN='Y' AND WITHDRAWAL_DATE IS NULL AND ACCOUNT_STATUS='01')</li>
 *   <li>{@code avgUsageRate}       — 활성 부여 기준 평균 사용률(%). 부여 0이면 0. 서비스에서 정수 반올림.</li>
 *   <li>{@code expiringSoon30}     — 30일 내 소멸 임박 활성 부여를 가진 직원 수 (DISTINCT USER_CD)</li>
 *   <li>{@code newGrantThisMonth}  — 이번 달 신규 부여 건수 (GRANT_DATE 이번 달, DEL_YN='N')</li>
 * </ul>
 */
@Getter
@Setter
public class LeaveDashboardMetricsVO {

    /** 전체 활성 직원 수 */
    private Integer totalEmployees;

    /** 평균 사용률 합계 산출용 — 전체 활성 부여 합 */
    private BigDecimal totalGranted;

    /** 평균 사용률 합계 산출용 — 전체 활성 사용 합 */
    private BigDecimal totalUsed;

    /** 소멸 임박(30일) 직원 수 */
    private Integer expiringSoon30;

    /** 이번달 신규 부여 건수 */
    private Integer newGrantThisMonth;
}
