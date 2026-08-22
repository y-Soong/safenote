package com.prafta.web.attd.attd09.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 입사일 기준 차액 조회 목록 응답 (경력인정 이원화 Phase 2 §2-2).
 * GET /attd09/leave-dashboard/shortfall/list.
 */
@Value
@Builder
public class ShortfallListResponse {

    /** 회사 정책 AXIS1=FISCAL_YEAR 여부. 'N'이면 rows는 항상 빈 배열(에러 아님 — 탭 비노출 판정용). */
    String fiscalYearYn;

    /** 조회 기준일 (YYYYMMDD) */
    String baseYmd;

    List<Row> rows;

    long totalCount;

    @Value
    @Builder
    public static class Row {
        String userCd;
        String userNm;
        String hireDate;
        BigDecimal hireBasisAccrual;
        BigDecimal actualAccrual;
        BigDecimal diff;
        BigDecimal coveredTotal;
        BigDecimal remainingShortfall;
    }
}
