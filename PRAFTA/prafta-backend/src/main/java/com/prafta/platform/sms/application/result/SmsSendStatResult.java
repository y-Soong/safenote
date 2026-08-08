package com.prafta.platform.sms.application.result;

/**
 * 특정 기간의 발송 상태 분포(Platform_05 현황 카드).
 *
 * <p>★집계만 제공한다. 개별 발송 행(휴대폰/인증번호/refKey)은 응답에 담지 않는다.
 *    {@code TB_SMS_AUTH_CODE} 에는 {@code CMPNY_CD} 가 없어 테넌트 경계를 걸 수단이 없기 때문에,
 *    개별 행을 노출하는 순간 회사 간 열람 문제가 생긴다(sec 백로그 4).
 *    <b>개별 발송 이력 조회 화면을 만들지 말 것.</b>
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
