package com.prafta.common.cmm.leave.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * 연차 현황 대시보드(attd09) 메트릭 카드 4종(응답용, 가공 완료).
 *
 * <p>{@code avgUsageRate}는 전체 활성 부여 합 대비 사용 합의 백분율(정수 반올림). 부여 0이면 0.
 */
@Getter
@Builder
public class LeaveDashboardMetricsResultVO {

    /** 전체 활성 직원 수 */
    private final int totalEmployees;

    /** 평균 사용률 (0~100 정수) */
    private final int avgUsageRate;

    /** 소멸 임박(30일) 직원 수 */
    private final int expiringSoon30;

    /** 이번달 신규 부여 건수 */
    private final int newGrantThisMonth;
}
