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
    /** prafta-com-008-B: 시도 → 노무수령거부 차단(현행 단일 생성 이벤트). */
    public static final String EVENT_BLOCKED = "BLOCKED";

    /** @deprecated com-008-B 차단 전환으로 신규 생성 안 함(기존 audit 데이터 호환용 보존). */
    @Deprecated
    public static final String EVENT_NOTICED = "NOTICED";
    /** @deprecated com-008-B 차단 전환으로 신규 생성 안 함(사후 감지 폐지). 보존만. */
    @Deprecated
    public static final String EVENT_CHECKIN_DETECTED = "CHECKIN_DETECTED";
    /** @deprecated com-008-B 차단 전환으로 신규 생성 안 함. 보존만. */
    @Deprecated
    public static final String EVENT_ADMIN_ALERTED = "ADMIN_ALERTED";

    // ── 차단 시도 유형 (tb_leave_refusal_log.ATTEMPT_TYPE, BLOCKED 부가) ──
    public static final String ATTEMPT_CHECK_IN = "CHECK_IN";
    public static final String ATTEMPT_CHECK_OUT = "CHECK_OUT";
    public static final String ATTEMPT_ATTD_CREATE = "ATTD_CREATE";
    public static final String ATTEMPT_ADMIN_ENTRY = "ADMIN_ENTRY";

    // ── SYS045 알림 유형 (tb_noti_outbox.NOTI_TYPE) ──
    /** prafta-com-008-B: 노무수령거부일 시도 차단 관리자 알림(현행). */
    public static final String NOTI_TYPE_BLOCK_ALERT = "LEAVE_REFUSAL_BLOCK_ALERT";

    /** @deprecated com-008-B 차단 전환으로 NOTI_TYPE_BLOCK_ALERT 로 대체. 보존만(audit 데이터 호환). */
    @Deprecated
    public static final String NOTI_TYPE_CHECKIN_ALERT = "LEAVE_REFUSAL_CHECKIN_ALERT";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── (구) 기능1 통지 / 출근감지 알림 템플릿 ──
    //   com-008-B-4 에서 제거: web 기능1(통지)·detect 사후감지 폐지로 호출부 0.
    //   (NOTICE_TITLE/NOTICE_BODY_FORMAT/CHECKIN_ALERT_TITLE/CHECKIN_ALERT_BODY_FORMAT 삭제 — BLOCK_ALERT_* 로 대체.)

    // ── 차단 메시지 템플릿(초안 §7 — 노무사 검토 후 확정. 본 상수만 교체) ──
    /**
     * 근로자용 노무수령거부 차단 안내(에러 메시지). 촉진 확정 연차일 출퇴근/근태 등록 시도 차단.
     * (날짜/근로자명은 노출하지 않는 일반 안내 — 클라이언트가 대상일을 자체 표시.)
     */
    public static final String BLOCK_POPUP_MESSAGE =
            "연차사용촉진으로 확정된 연차 사용일입니다. 회사는 금일 노무 제공을 수령하지 않으며, 출퇴근·근태 등록이 제한됩니다.";

    /** 관리자용 차단 시도 알림 제목. */
    public static final String BLOCK_ALERT_TITLE = "[노무수령거부일 시도]";
    /**
     * 관리자용 차단 시도 알림 본문 템플릿. {@code %s}=대상 근로자명(USER_NM 평문) · {@code %s}=대상일(YYYYMMDD).
     * 근로자명은 관리자 식별 목적의 최소 PII(평문 USER_NM)만 본문에 합성한다(DATA_PAYLOAD 에는 미포함).
     */
    public static final String BLOCK_ALERT_BODY_FORMAT =
            "%s님이 연차촉진 확정일(%s)에 출퇴근/근태 등록을 시도했습니다. 현장 확인 및 노무 미부여 조치가 필요합니다.";
}
