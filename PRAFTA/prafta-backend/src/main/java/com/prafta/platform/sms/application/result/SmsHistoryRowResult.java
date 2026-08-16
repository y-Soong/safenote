package com.prafta.platform.sms.application.result;

/**
 * Platform_05: SMS 발송 이력 1건(매퍼 원본 행).
 *
 * <p>★내부 운반체다 — 휴대폰 암호문({@code mblNoEnc})을 담고 있으므로 <b>그대로 응답하지 않는다.</b>
 * 서비스가 복호 후 마스킹하여 {@code SmsHistoryListResponse.Row} 로 옮긴다
 * ({@code SelfJoinHistoryRowResult} 와 동일 규약).
 *
 * <p>★★<b>담지 않는 컬럼(추가 금지)</b>: {@code AUTH_CD}(6자리 인증번호 평문 = 유효 자격증명),
 * {@code MBL_NO_HMAC}, {@code SEND_IP_HASH}, {@code SEND_REF_KEY}, {@code SEND_MSG_KEY},
 * {@code EXPIRED_AT}, {@code FAIL_LOCKED_AT}.
 * "SELECT 만 하고 안 내보내면 된다" 도 금지한다 — 실수 경로 자체를 만들지 않는다.
 *
 * <p>★record 필드 순서 = 매퍼 SELECT 컬럼 순서와 1:1 (MyBatis 위치 매핑 —
 * feedback_mybatis_record_column_order). 순서가 어긋나면 값이 밀려 담겨도 예외가 나지 않는다.
 */
public record SmsHistoryRowResult(
        Long smsId
        , String insertDate
        , String mblNoEnc
        , String purposeCd
        , String sendStatus
        , String sendDate
        , String verifiedYn
        , int failCnt
        , String sendErrCd
        , String sendErrMsg
        , String sendUserCd
) {
}
