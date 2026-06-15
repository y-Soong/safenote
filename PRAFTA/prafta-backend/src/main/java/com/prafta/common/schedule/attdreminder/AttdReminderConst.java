package com.prafta.common.schedule.attdreminder;

/**
 * 출근/퇴근 5분 전 리마인더(W4/W5) PUSH 공용 상수 (PRAFTA-APP-021-4).
 *
 * <p>분단위 cron 으로 (현재시각+5분)에 시작/종료하는 스케줄 보유자에게 리마인더를 적재한다.
 * 멱등 dedupKey(CHECKIN_REMIND_/CHECKOUT_REMIND_ + userCd_workYmd_workSeq)로 매분 재실행돼도 1건만.
 * 단일 타임존(서버 LocalTime 기준, §8-R 5). DATA_PAYLOAD 는 라우팅 키만.
 */
public final class AttdReminderConst {

    private AttdReminderConst() {
    }

    /** 리마인더 선행 시간(분) — "5분 전". */
    public static final int LEAD_MINUTES = 5;

    // ── SYS045 알림 유형 (tb_noti_outbox.NOTI_TYPE) ──
    public static final String NOTI_TYPE_CHECKIN = "ATTD_CHECKIN_REMINDER";
    public static final String NOTI_TYPE_CHECKOUT = "ATTD_CHECKOUT_REMINDER";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── 메시지 ──
    public static final String CHECKIN_TITLE = "[출근 시간 알림]";
    public static final String CHECKIN_BODY = "출근 시간 5분 전입니다.";
    public static final String CHECKOUT_TITLE = "[퇴근 시간 알림]";
    public static final String CHECKOUT_BODY = "퇴근 시간 5분 전입니다.";
}
