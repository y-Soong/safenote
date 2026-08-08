package com.prafta.common.cmm.sms.policy;

/**
 * 한 축(번호 / IP / 사용자)의 최근 1시간·1일 발송 건수.
 *
 * <p>축당 쿼리를 1회로 줄이려고 하나의 SELECT 에서 시간·일 카운트를 동시에 산출한다
 * ({@code WHERE INSERT_DATE >= NOW()-INTERVAL 1 DAY} 로 한 번 훑고, 시간 카운트는 CASE 로 접는다).
 *
 * <p>★record 매핑은 SELECT 컬럼 순서에 의존한다 — 매퍼 XML 의 {@code hourCnt, dayCnt} 순서를 유지할 것.
 *
 * @param hourCnt 최근 1시간 건수
 * @param dayCnt  최근 1일 건수
 */
public record SmsAxisCount(
    int hourCnt
    , int dayCnt
) {
}
