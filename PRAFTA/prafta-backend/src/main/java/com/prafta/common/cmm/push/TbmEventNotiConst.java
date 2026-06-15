package com.prafta.common.cmm.push;

/**
 * TBM 교육 시작/종료 통보(W3) PUSH 공용 상수 (PRAFTA-APP-021-3b).
 *
 * <p>수신 대상은 해당 세션에 <b>실제 입실한 참석자(enter)</b> 뿐이다(§8-R 2). DATA_PAYLOAD 는
 * 라우팅 키(type/sessionCd)만 직렬화한다(PII 미포함).
 */
public final class TbmEventNotiConst {

    private TbmEventNotiConst() {
    }

    // ── SYS045 알림 유형 (tb_noti_outbox.NOTI_TYPE) ──
    /** TBM 교육 시작(IN_PROGRESS 전이) — 입실 참석자 대상. */
    public static final String NOTI_TYPE_TBM_STARTED = "TBM_STARTED";
    /** TBM 교육 종료(COMPLETED 전이) — 입실 참석자 대상. */
    public static final String NOTI_TYPE_TBM_COMPLETED = "TBM_COMPLETED";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── 메시지 ──
    public static final String STARTED_TITLE = "[TBM 교육 시작]";
    public static final String STARTED_BODY = "참여 중인 TBM 교육이 시작되었습니다.";
    public static final String COMPLETED_TITLE = "[TBM 교육 종료]";
    public static final String COMPLETED_BODY = "참여한 TBM 교육이 종료되었습니다.";
}
