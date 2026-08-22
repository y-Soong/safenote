package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * 입사일 기준 차액 보전(법정 수기부여) 결과 (경력인정 이원화 Phase 2 §2-3, attd09).
 */
@Getter
@Builder
public class CoverGrantResultVO {

    /** 신규 생성된 부여 ID */
    private final String grantId;

    /** 실제 부여된 일수 */
    private final BigDecimal grantedDays;

    /** 부여 직후 남은 부족분 (요청 기준일 기준 서버 재계산치 - 이번 부여량) */
    private final BigDecimal remainingShortfallAfter;
}
