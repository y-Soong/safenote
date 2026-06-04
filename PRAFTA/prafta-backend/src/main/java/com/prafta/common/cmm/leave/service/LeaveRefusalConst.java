package com.prafta.common.cmm.leave.service;

/**
 * 노무수령거부 통지/감지/알림 공용 상수 (PRAFTA-COM-001).
 *
 * <p>SYS064 이벤트 유형 / SYS045 알림 유형 / 메시지 템플릿을 한 곳에 모은다.
 * 메시지 문구는 노무사 검토(선행조치 C) 확정 시 본 상수만 교체하면 된다.
 */
public final class LeaveRefusalConst {

    private LeaveRefusalConst() {
    }

    // ── SYS064 이벤트 유형 (tb_leave_refusal_log.EVENT_TYPE) ──
    public static final String EVENT_NOTICED = "NOTICED";
    public static final String EVENT_CHECKIN_DETECTED = "CHECKIN_DETECTED";
    public static final String EVENT_ADMIN_ALERTED = "ADMIN_ALERTED";

    // ── SYS045 알림 유형 (tb_noti_outbox.NOTI_TYPE) ──
    public static final String NOTI_TYPE_NOTICE = "LEAVE_REFUSAL_NOTICE";
    public static final String NOTI_TYPE_CHECKIN_ALERT = "LEAVE_REFUSAL_CHECKIN_ALERT";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── 메시지 템플릿(초안, §6) — 교체 용이하도록 상수화 ──
    /** 근로자용 통지 제목. */
    public static final String NOTICE_TITLE = "[연차 사용지정일 안내]";
    /**
     * 근로자용 통지 본문 템플릿. {@code {0}}=대상일(YYYYMMDD).
     * (근로자명은 PII 평문 보관 회피를 위해 본문에 합성하지 않고 consumer 가 렌더링 시점에 합성한다.)
     */
    public static final String NOTICE_BODY_FORMAT =
            "%s은(는) 미사용 연차 사용지정일입니다. 회사는 금일 노무 제공을 수령하지 않으며 업무가 부여되지 않습니다. 해당일은 연차휴가 사용일로 처리됩니다.";

    /** 관리자용 출근감지 알림 제목. */
    public static final String CHECKIN_ALERT_TITLE = "[노무수령거부일 출근 감지]";
    /**
     * 관리자용 출근감지 알림 본문 템플릿. {@code %s}=대상일(YYYYMMDD).
     * (대상 근로자명은 consumer 가 렌더링 시점에 합성.)
     */
    public static final String CHECKIN_ALERT_BODY_FORMAT =
            "노무수령거부 지정일(%s)에 출근 기록이 감지되었습니다. 현장 확인 및 노무 미부여 조치가 필요합니다.";
}
