package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * 차기 부여 예정 본연차 projection 결과 (prafta-com-011 가불 코어, read-only).
 *
 * <p>정책서/출처: {@code .claude/requests/common/prafta-com-011-decisions.md} §2·§3 (가불 한도/만료).
 *
 * <p>본연차 가불 한도와 만료일 산정을 위해, 기존 부여 엔진의 차기 회차 entitlement projection 결과를
 * 운반한다. {@code days}는 차기 부여 예정 본연차(STATUTORY_ANNUAL) + 근속가산(STATUTORY_TENURE_BONUS)
 * 예정 일수 합이고, {@code availFromYmd}/{@code availToYmd}는 차기 부여 발생일(입사 기념일 또는 회계연도
 * 시작)과 그 발생일 + AXIS6 유효개월로 산정한 정상 만료일이다(결정 §3).
 *
 * <p>차기 발생일이 없거나(입사일 미입력 등) 산정 불가면 {@code days}=0, ymd 는 null 일 수 있다.
 */
@Getter
@Builder
public class BorrowProjectionVO {

    /** 차기 부여 예정 본연차(+근속가산) 일수 합. 가불 본연차 한도의 모수(이미 가불분 차감 전). */
    private BigDecimal days;

    /** 차기 부여 발생일(=가불 GRANT 의 AVAIL_FROM 기준 만료 산정용, YYYYMMDD). 산정 불가면 null. */
    private String availFromYmd;

    /** 차기 부여 본연차의 정상 만료일(발생일 + AXIS6 유효개월, YYYYMMDD). 산정 불가면 null. */
    private String availToYmd;
}
