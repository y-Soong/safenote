package com.prafta.platform.location.application.result;

/**
 * 플랫폼 위치열람 SMS 인증 상태(10분 유효) 판정 결과.
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서 의존 — XML SELECT 순서와 일치 유지.
 */
public record PlatformSmsVerifiedResult(
    Long smsId          // 통과한 인증 레코드(열람 로그 SMS_AUTH_ID 스냅샷)
    , String verifiedAt // 인증 통과 일시(UPDATE_DATE, 'YYYY-MM-DD HH:mm:ss')
    , Long remainSec    // 인증 상태 잔여 초(10분 창 기준, 음수 없음 — 쿼리에서 보정)
) {
}
