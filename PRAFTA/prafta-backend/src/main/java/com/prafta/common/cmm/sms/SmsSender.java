package com.prafta.common.cmm.sms;

/**
 * SMS 발송 게이트웨이(벤더 중립 인터페이스).
 *
 * <p>호출부는 본 인터페이스에만 의존한다. 뿌리오 구현 세부(토큰/엔드포인트/응답 스키마)가
 *    호출부로 새지 않게 하여 향후 벤더 교체 시 구현체 한 곳만 바꾸면 되게 한다(요청서 §7-1).
 *
 * <p>구현체는 실패를 예외로 던지지 않는다. 항상 {@link SmsSendResult} 를 반환한다.
 */
public interface SmsSender {

    /**
     * 실발송 가능 여부(게이트 ON + 계정/인증키/발신번호 설정 완료).
     * false 면 {@link #send(SmsSendRequest)} 는 항상 {@link SmsSendStatus#SKIPPED} 를 반환한다.
     */
    boolean isEnabled();

    /**
     * 문자 1건 발송(동기).
     *
     * @param request 발송 요청
     * @return 발송 결과. ★예외를 던지지 않는다(실패도 결과로 표현).
     */
    SmsSendResult send(SmsSendRequest request);
}
