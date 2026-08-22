package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 입사일 변경 회수(prafta-032 D5) 대상 ACTIVE 법정(STATUTORY_*) 부여 1행.
 *
 * <p>회수 우선순위 정렬용 최소 컬럼만 담는다(소멸일/부여일/부여ID + 일수). 정렬은 SQL이 수행하고
 * 서비스는 순회하며 회수량을 차감한다. {@code tb_user_leave_use} 사용 이력(USED_DAYS, FK)은 불변.
 */
@Getter
@Setter
public class LeaveGrantRecallRowVO {

    /** 부여 ID (PK) */
    private String grantId;

    /** 부여 분류 (STATUTORY_*) */
    private String grantType;

    /** 부여 일수 */
    private BigDecimal grantDays;

    /** 사용 일수 캐시 */
    private BigDecimal usedDays;

    /** 부여 일자 (YYYYMMDD) */
    private String grantDate;

    /** 사용 가능 종료일 = 소멸일 (YYYYMMDD) */
    private String availToDate;

    /**
     * 멱등키 (경력인정 이원화 Phase 2 §2-3 P2-6 ③). 입사일 변경 회수 스냅샷에서 법정 수기부여(_COVER,
     * GRANT_TYPE='STATUTORY_ANNUAL'과 동일해 grantType만으로는 구분 불가)를 식별하기 위한 구분 필드.
     * {@code IDEMPOTENCY_KEY LIKE '%_COVER'} 이면 보전 부여.
     */
    private String idempotencyKey;
}
