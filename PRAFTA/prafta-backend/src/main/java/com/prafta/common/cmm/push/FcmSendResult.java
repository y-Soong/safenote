package com.prafta.common.cmm.push;

/**
 * FCM 단건 전송 결과 분기 (PRAFTA-COM-002).
 *
 * <p>{@link FcmClient#send} 가 반환한다. 서비스는 이 3분기로 행 상태를 결정한다.
 * <ul>
 *   <li>{@link #SUCCESS} — 정상 발송. 행은 SENT.</li>
 *   <li>{@link #INVALID_TOKEN} — 영구 무효(FCM UNREGISTERED / INVALID_ARGUMENT).
 *       해당 디바이스 토큰을 soft-delete 하고 재시도하지 않는다.</li>
 *   <li>{@link #TRANSIENT_FAILURE} — 일시 실패(네트워크/5xx/UNAVAILABLE 등).
 *       재시도 대상(다음 주기). maxRetry 초과 시 FAILED.</li>
 * </ul>
 */
public enum FcmSendResult {
    SUCCESS,
    INVALID_TOKEN,
    TRANSIENT_FAILURE
}
