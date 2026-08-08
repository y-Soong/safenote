package com.prafta.common.cmm.sms;

/**
 * SMS 발송 결과 상태. TB_SMS_AUTH_CODE.SEND_STATUS 컬럼값과 1:1 대응한다.
 *
 * <p>DB 에는 발송 전 기본값 {@code PENDING} 이 별도로 존재한다(INSERT 시점 값).
 *    본 enum 은 "발송 시도 이후" 확정 상태만 표현하므로 PENDING 을 포함하지 않는다.
 */
public enum SmsSendStatus {

    /** 발송 성공(벤더가 접수 확인). */
    SENT,
    /** 발송 실패(연결 실패/타임아웃/벤더 거절). 사용자에게 실패 응답을 내려야 한다. */
    FAILED,
    /** 게이트 OFF 또는 설정 미완으로 실제 발송을 시도하지 않음. 사용자 응답은 기존과 동일한 성공. */
    SKIPPED
}
