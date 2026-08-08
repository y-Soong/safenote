package com.prafta.common.cmm.sms;

/**
 * SMS 발송 실패 분류.
 *
 * <p>실패 원인 문자열(벤더 원문 코드)은 DB 적재 전용이므로 분기 판단에 쓰지 않는다.
 *    사용자 노출 에러코드({@code SmsErrorCode})는 본 분류로만 결정한다.
 */
public enum SmsFailureKind {

    /** 게이트웨이 토큰 발급 실패(계정/인증키 오류 등) → SMS_502_003. */
    TOKEN,
    /** 연결 실패/타임아웃/응답 파싱 불가 등 전송 계층 실패 → SMS_502_001. */
    TRANSPORT,
    /** 벤더가 명시적으로 거절(발신번호 미등록·잔액 부족 등) → SMS_502_002. */
    VENDOR_REJECTED
}
