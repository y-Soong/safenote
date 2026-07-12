package com.prafta.web.attd.attd09.dto.response;

import java.util.List;

import com.prafta.common.cmm.leave.vo.LeaveDashboardItemVO;
import com.prafta.common.cmm.leave.vo.LeaveDashboardMetricsResultVO;
import com.prafta.common.cmm.leave.vo.PagingMetaVO;

import lombok.Builder;
import lombok.Value;

/**
 * 연차 현황 대시보드 목록 응답.
 * GET /attd09/leave-dashboard/list.
 */
@Value
@Builder
public class LeaveDashboardResponse {

    /** 메트릭 카드 4종 */
    LeaveDashboardMetricsResultVO metrics;

    /** 직원 목록 (현재 페이지) */
    List<LeaveDashboardItemVO> list;

    /** 페이징 메타 */
    PagingMetaVO paging;

    /** LC-07(표기): 현재(오늘) 기준 1일 환산시간(분, 기본 480) — FE "N일 H시간 M분" 조립용(additive). */
    int convMinutes;
}
