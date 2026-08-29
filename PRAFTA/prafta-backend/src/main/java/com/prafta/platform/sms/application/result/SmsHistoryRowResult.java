package com.prafta.platform.sms.application.result;

/**
 * Platform_05: SMS 발송 이력 1건(매퍼 원본 행).
 *
 * <p>★내부 운반체다 — 휴대폰 암호문({@code mblNoEnc})을 담고 있으므로 <b>그대로 응답하지 않는다.</b>
 * 서비스가 복호 후 마스킹하여 {@code SmsHistoryListResponse.Row} 로 옮긴다
 * ({@code SelfJoinHistoryRowResult} 와 동일 규약).
 *
 * <p>★★<b>담지 않는 컬럼(추가 금지)</b>: {@code MBL_NO_HMAC}, {@code SEND_IP_HASH},
 * {@code SEND_REF_KEY}, {@code SEND_MSG_KEY}, {@code EXPIRED_AT}, {@code FAIL_LOCKED_AT}.
 * "SELECT 만 하고 안 내보내면 된다" 도 금지한다 — 실수 경로 자체를 만들지 않는다.
 *
 * <p>★[2026-08-30 방침 변경] {@code authCdSkipped} 는 {@code SEND_STATUS='SKIPPED'}(게이트 OFF 로
 * 실발송이 안 된 행)에 한해 매퍼 CASE 게이트로 채워진다 — 게이트 OFF 환경에서 운영자가 화면에서
 * 인증번호를 읽어 검증 흐름을 테스트하기 위함. SENT/FAILED/PENDING 행은 항상 null 이다.
 *
 * <p>★record 필드 순서 = 매퍼 SELECT 컬럼 순서와 1:1 (MyBatis 위치 매핑 —
 * feedback_mybatis_record_column_order). 순서가 어긋나면 값이 밀려 담겨도 예외가 나지 않는다.
 */
public record SmsHistoryRowResult(
        Long smsId
        // [2026-08-30] 시각 2종은 epoch 초 — 운영 DB 시계=UTC 라 문자열 그대로는 KST-9h 표시.
        //              서비스가 Asia/Seoul 로 포맷해 응답에 담는다. sendEpoch 는 결과 미확정이면 null.
        , Long insertEpoch
        , String mblNoEnc
        , String purposeCd
        , String sendStatus
        , Long sendEpoch
        , String verifiedYn
        , int failCnt
        , String sendErrCd
        , String sendErrMsg
        , String sendUserCd
        , String authCdSkipped
) {
}
