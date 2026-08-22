package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * 입사일 기준 차액 조회 1행 (경력인정 이원화 Phase 2 §2-2, Attd_09_Shortfall.vue).
 *
 * <p>정책서 출처: 작업지시서 §2-2 / plan §F-1. PII는 사번/성명만(security §5).
 */
@Getter
@Builder
public class ShortfallRowVO {

    private final String userCd;
    private final String userNm;

    /** 입사일 (YYYYMMDD) */
    private final String hireDate;

    /** 입사일 기준 "정답" 누적 (엔진 computeHireBasisAccrual) */
    private final BigDecimal hireBasisAccrual;

    /** 실제 부여 누적 (P-12: live 법정 부여 총량 selectStatutoryGrantedLiveTotal — 사용·만료 무관, _COVER 포함) */
    private final BigDecimal actualAccrual;

    /** 차액 = 정답 누적 − 실제 부여 누적 (음수 그대로 — 회계연도 트랙 우세 구간) */
    private final BigDecimal diff;

    /** 기보전 합 (_COVER 부여 합, 참고 컬럼) */
    private final BigDecimal coveredTotal;

    /** 남은 부족분 = diff (기보전은 actualAccrual에 이미 포함되어 있어 이중 차감하지 않는다) */
    private final BigDecimal remainingShortfall;
}
