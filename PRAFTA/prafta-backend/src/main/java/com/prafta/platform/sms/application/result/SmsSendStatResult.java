package com.prafta.platform.sms.application.result;

/**
 * 특정 기간의 발송 상태 분포(Platform_05 현황 카드).
 *
 * <p>★이 record 자체는 <b>집계만</b> 제공한다. 개별 발송 행(휴대폰/인증번호/refKey)을 여기에 담지 않는다.
 *
 * <p>★★<b>[방침 변경 / 2026-08-16]</b> 종전의 "개별 발송 이력 조회 화면을 만들지 말 것" 은 대체됐다.
 *    개별 발송 이력은 <b>플랫폼 운영자 콘솔에 한해</b> 노출한다(Platform_05 발송 이력 탭,
 *    {@code SmsHistoryListResponse} — 휴대폰은 서버에서 복호 후 마스킹).
 *    다만 <b>인증번호({@code AUTH_CD}) · {@code MBL_NO_HMAC} · {@code SEND_IP_HASH} 는 여전히 제외</b> 다 —
 *    만료 전 인증번호는 그 자체로 계정 탈취(비밀번호 재설정 통과)에 쓰이는 유효 자격증명이다.
 *    {@code TB_SMS_AUTH_CODE} 에는 {@code CMPNY_CD} 가 없어 테넌트 경계를 걸 수단이 없으므로(sec 백로그 4),
 *    개별 행 조회는 <b>{@code /platformApi} 게이트 뒤에서만</b> 성립한다.
 *    {@code com.prafta.web.*} / {@code com.prafta.app.*} 에서 재사용 금지.
 *
 * <p>★record 매핑은 SELECT 컬럼 순서 의존 — 매퍼 XML 순서와 함께 유지할 것.
 */
public record SmsSendStatResult(
    int sent
    , int failed
    , int skipped
    , int pending
    , int total
) {
}
