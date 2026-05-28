package com.prafta.common.cmm.leave.vo;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 연차 현황 대시보드(attd09) 목록 조회 결과 집합(서비스 산출).
 *
 * <p>프론트 계약: {@code { metrics, list, paging }}.
 */
@Getter
@Builder
public class LeaveDashboardResultVO {

    /** 메트릭 카드 4종 */
    private final LeaveDashboardMetricsResultVO metrics;

    /** 직원 목록 (현재 페이지) */
    private final List<LeaveDashboardItemVO> list;

    /** 페이징 메타 */
    private final PagingMetaVO paging;
}
