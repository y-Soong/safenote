package com.prafta.common.cmm.sms;

/**
 * SMS 발송 요청(벤더 중립).
 *
 * @param refKey      요청 추적키(32자 이내). TB_SMS_AUTH_CODE.SEND_REF_KEY 와 동일 값이며 결과 UPDATE 의 조인키다.
 * @param toPhoneNorm 수신번호(하이픈 없는 숫자만 — {@code Normalizers.normalizePhone} 결과)
 * @param content     발송 본문. ★고정 템플릿 + 서버 생성 인증번호만 들어간다(사용자 입력 유입 금지).
 *                    ★인증번호를 포함하므로 어떤 로그에도 출력하지 않는다.
 */
public record SmsSendRequest(
    String refKey
    , String toPhoneNorm
    , String content
) {

    /**
     * SMS2-D1(sec L-1): {@code toString()} 재정의 — 인증번호·휴대폰 평문 노출 차단.
     *
     * <p>record 의 자동 생성 {@code toString()} 은 모든 컴포넌트 값을 그대로 찍는다.
     *    {@code log.debug("{}", request)} 한 줄이나 예외 메시지 조립 한 번으로 인증번호가 파일 로그에 박힌다
     *    ({@code SmsProperties} 가 Lombok {@code @ToString} 을 배제한 것과 동일한 이유).
     *    운영은 {@code logging.level.com.prafta=DEBUG} 이므로 실제 위험이다.
     */
    @Override
    public String toString() {
        return "SmsSendRequest[refKey=" + refKey
            + ", toPhoneLast4=" + com.prafta.common.security.normalize.Normalizers.last4(toPhoneNorm)
            + ", content=***]";
    }
}
