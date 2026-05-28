package com.prafta.web.attd.attd09.dto.request;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 연차 수동 부여(단일) 요청 body.
 * POST /attd09/leave-grant/manual-grant.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.8
 * <p>cmpnyCd는 요청 body로 받지 않는다(JWT 스코프만 신뢰 — 가드레일 3).
 */
@Getter
@Setter
@NoArgsConstructor
public class ManualGrantRequest {

    /** 대상 사용자 코드 */
    private String userCd;

    /** 연차 코드 (수동 부여 가능 휴가 종류) */
    private String leaveCd;

    /** 부여 일수 (양수, 0.5일 단위) */
    private BigDecimal grantDays;

    /** 사용 가능 시작일 (YYYYMMDD) */
    private String availFromDate;

    /** 부여 사유 (선택) */
    private String reason;
}
