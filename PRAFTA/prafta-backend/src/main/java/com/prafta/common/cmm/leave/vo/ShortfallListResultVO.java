package com.prafta.common.cmm.leave.vo;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 입사일 기준 차액 조회 목록 결과 (경력인정 이원화 Phase 2 §2-2).
 */
@Getter
@Builder
public class ShortfallListResultVO {

    /** 회사 정책 AXIS1=FISCAL_YEAR 여부. 'N'이면 rows는 항상 빈 배열(에러 아님 — 탭 비노출 판정용). */
    private final String fiscalYearYn;

    /** 조회 기준일 (YYYYMMDD) */
    private final String baseYmd;

    private final List<ShortfallRowVO> rows;

    private final long totalCount;
}
