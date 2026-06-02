package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * tb_user_leave_grant 행의 STATUS 재평가에 필요한 핵심 컬럼만 담는 VO.
 *
 * <p>본 VO는 {@link com.prafta.common.cmm.leave.service.LeaveGrantStatusService}에서만 사용한다.
 * 일반 조회/응답 용도가 아닌 내부 상태 동기화 전용이므로 외부 모듈에서 직접 참조하지 않는다.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.8
 * (STATUS ↔ EXPIRE_YN/DEL_YN 단방향 매핑표)
 */
@Getter
@Setter
public class LeaveGrantStatusVO {

    /** 부여 ID (PK) */
    private String grantId;

    /** 회사 코드 */
    private String cmpnyCd;

    /** 사용자 코드 */
    private String userCd;

    /** 연차 코드 */
    private String leaveCd;

    /** STATUS 현재 값 (ACTIVE/EXHAUSTED/EXPIRED/CANCELED) */
    private String status;

    /** EXPIRE_YN 현재 값 (Y/N) */
    private String expireYn;

    /** DEL_YN 현재 값 (Y/N) */
    private String delYn;

    /** 부여 일수 (decimal(5,1)) */
    private BigDecimal grantDays;

    /** 사용 일수 (decimal(5,1)) */
    private BigDecimal usedDays;

    /** 사용 가능 종료일 (YYYYMMDD 형식 varchar(8)) */
    private String availToDate;
}
