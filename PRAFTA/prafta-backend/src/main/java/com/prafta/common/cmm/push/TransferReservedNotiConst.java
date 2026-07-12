package com.prafta.common.cmm.push;

/**
 * 사용자 소속이동 예약 통보 PUSH 공용 상수 (PRAFTA-WEB_001-3, Terminal C).
 *
 * <p>master/hr 가 다른 사용자를 소속이동 예약(등록)한 직후, 대상자 본인에게 통보한다.
 * 등록 트랜잭션 커밋 이후에만 적재하며(afterCommit), 발송 실패가 등록을 롤백하지 않는다(best-effort).
 * 동일 예약은 {@code DEDUP_KEY} 로 1회만 적재된다(오발송 방지, 공통 §10.3).
 *
 * <p>BODY 에는 요청 5-1 에 따라 이동일 + 지정 항목(사업장/부서/기본근무타입/사유)을 담는다.
 * 사업장명/부서명/근무타입명/사유는 개인정보(PII: 이름/휴대폰/이메일)가 아니므로 본문 표시를 허용한다.
 * DATA_PAYLOAD 는 라우팅 키(예약 ID)만 담는다.
 *
 * <p>opt-out: NOTI_TYPE 기반 발송 억제는 워커({@code PushSenderServiceImpl}) enforce 경로가 자동 수행한다.
 */
public final class TransferReservedNotiConst {

    private TransferReservedNotiConst() {
    }

    /** SYS045 알림 유형: 소속이동 예약 통보(대상자 본인). */
    public static final String NOTI_TYPE = "TRANSFER_RESERVED";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── 메시지 ──
    public static final String TITLE = "[소속이동 안내]";

    /** DEDUP_KEY 접두 — UNIQUE(CMPNY_CD, DEDUP_KEY)와 결합해 예약 1건당 1회만 적재. */
    public static final String DEDUP_PREFIX = "TRANSFER_RESERVED_";

    /** BODY/사유 표시 최대 길이(본문 비대화 방지). */
    public static final int BODY_REASON_MAX_LEN = 200;
}
