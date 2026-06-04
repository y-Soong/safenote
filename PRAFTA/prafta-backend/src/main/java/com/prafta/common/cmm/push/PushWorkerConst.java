package com.prafta.common.cmm.push;

/**
 * FCM 전송 워커 공용 상수 (PRAFTA-COM-002).
 *
 * <p>워커 주체 식별자와 표준 ERROR_MSG 코드를 단일 출처로 둔다.
 */
public final class PushWorkerConst {

    private PushWorkerConst() {
    }

    /** 상태전이 INSERT_NO/UPDATE_NO 주체(스케줄러, 사용자 JWT 무관). */
    public static final String WORKER_ACTOR = "PUSH_WORKER";

    /** tb_noti_outbox.ERROR_MSG 컬럼 길이(varchar500) — 초과분 substring 가드. */
    public static final int ERROR_MSG_MAX_LEN = 500;

    /** 토큰 0건(대상 디바이스 없음) → 즉시 FAILED. */
    public static final String ERR_NO_DEVICE_TOKEN = "NO_DEVICE_TOKEN";

    /** 모든 디바이스 토큰이 무효(soft-delete됨) → FAILED. */
    public static final String ERR_ALL_TOKENS_INVALID = "ALL_TOKENS_INVALID";

    /** DATA_PAYLOAD json 파싱 실패 → 재시도 무의미, FAILED. */
    public static final String ERR_INVALID_PAYLOAD = "INVALID_PAYLOAD";
}
