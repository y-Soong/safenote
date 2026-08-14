package com.prafta.app.leave.leaveflow.result;

import java.math.BigDecimal;

/**
 * prafta-leavemulti: 잔여 배정 시뮬레이션용 부여 1건 (유효기간 동반).
 *
 * <p><b>왜 유효기간이 필요한가</b> — 잔여는 날짜마다 다르다. 실제 차감 쿼리
 * ({@code selectDeductibleGrants})가 {@code AVAIL_FROM_DATE <= 날짜 <= AVAIL_TO_DATE} 로 거르기 때문이다.
 * 운영 실측상 월차(SYS_MONTHLY)는 부여마다 발생일·만료일이 제각각이고, 한 사람이 만료일이 다른 부여를
 * 여럿 보유하며, 미래에 발생하는 부여도 존재한다.
 *
 * <p>따라서 기간신청에서 <b>"총 N일 ≤ 잔여" 단순 비교는 양방향으로 틀린다</b>.
 * (앞 날짜에만 쓸 수 있는 만료 임박 부여를 과대/과소 계상)
 * 날짜 오름차순으로 그 날짜에 유효한 부여를 만료 임박순으로 1일씩 배정하는 시뮬레이션이 필요하다.
 *
 * <p>정렬은 실제 차감과 동일하게 {@code AVAIL_TO_DATE ASC, GRANT_DATE ASC, GRANT_ID ASC} 이며,
 * 시뮬레이션의 그리디 배정도 {@code resolveGeneralCharges} 와 동일 규칙(앞에서부터 min(잔여, 필요))이다.
 * — 미리보기 판정과 실제 차감이 갈리지 않게 하는 것이 핵심이다.
 *
 * <p>미리보기/사전판정 전용이므로 <b>행 잠금(FOR UPDATE)을 걸지 않는다</b>.
 *
 * <p>⚠️ MyBatis 위치매핑 — record 필드 순서 = SELECT 컬럼 순서.
 */
public record RangeGrantRow(
        String grantId
        , BigDecimal grantDays
        , BigDecimal usedDays
        /** 사용 가능 시작일 (YYYYMMDD) — 이 날짜 이전에는 못 쓴다(미래 발생 부여 존재) */
        , String availFromDate
        /** 사용 가능 종료일 (YYYYMMDD) — 이 날짜 이후에는 못 쓴다(구간 중간 만료 존재) */
        , String availToDate
) {
    /** 남은 일수 (GRANT_DAYS - USED_DAYS). null 은 0 으로 본다. */
    public BigDecimal remaining() {
        BigDecimal g = (grantDays == null) ? BigDecimal.ZERO : grantDays;
        BigDecimal u = (usedDays == null) ? BigDecimal.ZERO : usedDays;
        return g.subtract(u);
    }

    /** 해당 날짜에 이 부여를 쓸 수 있는지 (실제 차감 쿼리의 기간 술어와 동일). */
    public boolean usableOn(String ymd) {
        return availFromDate != null && availToDate != null
                && availFromDate.compareTo(ymd) <= 0
                && availToDate.compareTo(ymd) >= 0;
    }
}
