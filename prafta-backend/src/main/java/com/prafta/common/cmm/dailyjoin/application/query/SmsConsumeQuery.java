package com.prafta.common.cmm.dailyjoin.application.query;

/**
 * 일일사용자 회원가입 - 가입 시점 SMS 인증 재검증/소진 대상 조회 쿼리.
 * TB_SMS_AUTH_CODE 에서 휴대폰 HMAC + 인증번호 기준 인증완료(VERIFIED_YN='Y') 행의 SMS_ID 를 조회한다.
 */
public record SmsConsumeQuery(
    String mblNoHmac
    , String certNo
) {
    public static SmsConsumeQuery of(String mblNoHmac, String certNo) {
        return new SmsConsumeQuery(mblNoHmac, certNo);
    }
}
