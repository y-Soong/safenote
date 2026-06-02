package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code tb_user_leave_grant} 수동 부여 INSERT 1건의 운반체(attd09).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.8
 *
 * <p>수동 부여 고정값:
 * <ul>
 *   <li>{@code grantType} = 'MANUAL_OTHER' (법정외 분류, SYS035)</li>
 *   <li>{@code grantByType} = 관리자 수동 부여 코드 (GRANT_BY_TYPE 컬럼 varchar(2) 제약 — 서비스에서 세팅)</li>
 *   <li>{@code policySeq} = NULL (수동 부여)</li>
 *   <li>{@code status} = 'ACTIVE', {@code delYn} = 'N', {@code usedDays} = 0.0</li>
 *   <li>{@code idempotencyKey} = "{USER_CD}_{TIMESTAMP}_MANUAL"</li>
 * </ul>
 */
@Getter
@Setter
public class LeaveGrantInsertVO {

    /** 부여 ID (PK, varchar(20)) — 서비스에서 채번하여 세팅 */
    private String grantId;

    /** 회사 코드 */
    private String cmpnyCd;

    /** 사용자 코드 */
    private String userCd;

    /** 연차 코드 */
    private String leaveCd;

    /** 부여 분류 (MANUAL_OTHER) */
    private String grantType;

    /** 부여 일수 */
    private BigDecimal grantDays;

    /** 사용 일수 캐시 (0.0) */
    private BigDecimal usedDays;

    /** 부여 사유 */
    private String grantReason;

    /** 부여 방식 (GRANT_BY_TYPE, varchar(2)) */
    private String grantByType;

    /** 적용 정책 (수동 부여는 NULL) */
    private Long policySeq;

    /** 부여 일자 (YYYYMMDD) */
    private String grantDate;

    /** 사용 가능 시작일 (YYYYMMDD) */
    private String availFromDate;

    /** 사용 가능 종료일 (YYYYMMDD) */
    private String availToDate;

    /** 멱등성 키 */
    private String idempotencyKey;

    /** 상태 (ACTIVE) */
    private String status;

    /** 등록자 */
    private String insertNo;
}
