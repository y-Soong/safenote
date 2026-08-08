package com.prafta.common.cmm.sms;

import java.util.regex.Pattern;

/**
 * SMS 발송 결과(벤더 중립).
 *
 * <p>★실패를 예외로 던지지 않고 결과 객체로 표현한다. 예외/결과 이중 채널이 되면
 *    "실패했는데 SEND_STATUS 를 기록하지 못하는" 경로가 생긴다. 상위(디스패처)가
 *    결과를 먼저 기록한 뒤 필요 시 ApiException 으로 전환한다.
 *
 * @param status      발송 상태
 * @param messageKey  벤더 응답 식별자(추후 도달결과 대사용). 실패/스킵 시 null
 * @param errCd       실패 코드(벤더 원문). 성공/스킵 시 null
 * @param errMsg      실패 사유(벤더 원문, 500자 절단). ★인증번호·휴대폰 평문을 담지 않는다. 성공/스킵 시 null
 * @param failureKind 실패 분류(사용자 노출 에러코드 결정용). 성공/스킵 시 null
 */
public record SmsSendResult(
    SmsSendStatus status
    , String messageKey
    , String errCd
    , String errMsg
    , SmsFailureKind failureKind
) {

    /** SEND_ERR_MSG 컬럼 길이(varchar 500) — 초과 시 1406 방지를 위해 Java 단에서 절단한다. */
    private static final int ERR_MSG_MAX_LEN = 500;

    /**
     * SEND_ERR_CD 컬럼 길이(varchar 50) — SMS2-D1(qa D-3).
     * ★미절단이면 벤더가 긴 코드 문자열을 주는 순간 UPDATE 가 1406 으로 실패하고,
     *   {@code SmsSendResultRecorder} 가 그 예외를 삼켜 행이 PENDING 으로 남는다.
     *   PENDING 은 레이트리밋 카운트에 포함되므로 "문자를 못 받았는데 차단까지 되는" 상태가 된다.
     */
    private static final int ERR_CD_MAX_LEN = 50;

    /**
     * 스크러빙 규칙 ①: 9자리 이상 연속 숫자 → {@code ***}.
     * 휴대폰번호(01012345678)·계정번호 등 식별 가능한 숫자열을 통째로 지운다.
     */
    private static final Pattern LONG_DIGITS = Pattern.compile("\\d{9,}");

    /**
     * 스크러빙 규칙 ②: 앞뒤가 숫자가 아닌 정확히 6자리 숫자 → {@code ******}.
     * 벤더가 요청 본문을 에코하는 경우 인증번호가 그대로 섞여 들어올 수 있다.
     */
    private static final Pattern AUTH_CODE_DIGITS = Pattern.compile("(?<!\\d)\\d{6}(?!\\d)");

    /** 발송 성공. */
    public static SmsSendResult sent(String messageKey) {
        return new SmsSendResult(SmsSendStatus.SENT, messageKey, null, null, null);
    }

    /**
     * 발송 실패.
     *
     * <p>SMS2-D1(sec H-1): 벤더 원문을 그대로 적재하면 응답 바디에 섞인 수신번호·인증번호가
     *    {@code SEND_ERR_CD}/{@code SEND_ERR_MSG} 에 평문으로 남는다(운영은 TRACE+p6spy 상태라 SQL 로그에도 남는다).
     *    ★반드시 <b>스크럽 → 절단</b> 순서로 처리한다. 절단을 먼저 하면 마스킹 대상 숫자열이 잘려
     *      정규식이 걸리지 않고 평문 일부가 그대로 살아남는다.
     *    ★{@code errCd} 에도 동일 적용한다 — {@code resolveVendorCode()} 가 {@code status} 필드까지
     *      후보로 훑기 때문에 코드 자리에 값이 섞일 수 있다.
     */
    public static SmsSendResult failed(SmsFailureKind kind, String errCd, String errMsg) {
        return new SmsSendResult(
            SmsSendStatus.FAILED
            , null
            , truncate(scrub(errCd), ERR_CD_MAX_LEN)
            , truncate(scrub(errMsg), ERR_MSG_MAX_LEN)
            , kind
        );
    }

    /** 게이트 OFF/설정 미완으로 발송 미시도. */
    public static SmsSendResult skipped() {
        return new SmsSendResult(SmsSendStatus.SKIPPED, null, null, null, null);
    }

    /** PII·인증번호 마스킹. ★절단보다 먼저 호출되어야 한다. */
    private static String scrub(String v) {
        if (v == null) {
            return null;
        }
        String masked = LONG_DIGITS.matcher(v).replaceAll("***");
        return AUTH_CODE_DIGITS.matcher(masked).replaceAll("******");
    }

    private static String truncate(String v, int maxLen) {
        if (v == null) {
            return null;
        }
        return v.length() <= maxLen ? v : v.substring(0, maxLen);
    }
}
