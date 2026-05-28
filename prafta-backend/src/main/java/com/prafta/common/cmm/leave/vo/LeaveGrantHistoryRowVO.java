package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 직원별 연차 상세(attd09)의 부여 이력 1행(매퍼 결과).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.1 / §8.5.8
 *
 * <p>{@code natureBadge}는 GRANT_TYPE prefix로 산출: STATUTORY_* → 'LEGAL', MANUAL_* → 'NON_LEGAL'.
 * 정렬은 GRANT_DATE 내림차순(매퍼).
 */
@Getter
@Setter
public class LeaveGrantHistoryRowVO {

    /** 부여 ID (PK) — 회수(soft cancel) 대상 식별용 (PRAFTA-031) */
    private String grantId;

    /** 부여일 (YYYYMMDD) */
    private String grantDate;

    /** 구분 배지 ('LEGAL' / 'NON_LEGAL') */
    private String natureBadge;

    /** 부여 사유 */
    private String reason;

    /** 부여 일수 */
    private BigDecimal granted;

    /** 사용 일수 */
    private BigDecimal used;

    /** 잔여 일수 (granted - used) */
    private BigDecimal remaining;

    /** 만료일 (AVAIL_TO_DATE, YYYYMMDD) */
    private String expiresAt;

    /** 상태[SYS040] ACTIVE/EXHAUSTED/EXPIRED/CANCELED */
    private String status;

    /** 사용 일수 캐시 (PRAFTA-031, 회수 가능 판정 보조 노출). */
    private BigDecimal usedDays;

    /** 부여 방식 [SYS043] 01:자동 / 02:관리자 수동 (PRAFTA-031). */
    private String grantByType;

    /**
     * 회수(soft cancel) 가능 여부 (PRAFTA-031). SQL에서 산출:
     * {@code GRANT_TYPE LIKE 'MANUAL_%' AND GRANT_BY_TYPE='02' AND STATUS='ACTIVE' AND USED_DAYS=0 AND DEL_YN='N'}.
     */
    private boolean canRecall;
}
