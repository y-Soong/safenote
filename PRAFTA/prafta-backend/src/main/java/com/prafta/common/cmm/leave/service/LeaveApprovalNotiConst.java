package com.prafta.common.cmm.leave.service;

/**
 * 연차 결재 PUSH(차례 도래 / 무결재 사용 통보) 공용 상수 (PRAFTA-COM-004).
 *
 * <p>SYS045 알림 유형 / 채널·상태 / 메시지 템플릿을 한 곳에 모은다.
 * 메시지 문구는 추후 노무 검토 시 본 상수만 교체하면 된다(LeaveRefusalConst 와 동일 철학).
 *
 * <p>본문에 합성하는 신청자명은 평문 {@code USER_NM} 조회값이다(AES-GCM 복호화 불필요).
 */
public final class LeaveApprovalNotiConst {

    private LeaveApprovalNotiConst() {
    }

    // ── SYS045 알림 유형 (tb_noti_outbox.NOTI_TYPE) ──
    /** 시나리오 A: 연차 결재 차례 도래(결재자 대상). */
    public static final String NOTI_TYPE_APPROVAL_TURN = "LEAVE_APPROVAL_TURN";
    /** 시나리오 B: 무결재 연차 사용 통보(노드 관리자 대상). */
    public static final String NOTI_TYPE_USED_NO_APRV = "LEAVE_USED_NO_APRV";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── 사용 단위 [SYS025] (본문 분기용) ──
    public static final String UNIT_FULL = "00";
    public static final String UNIT_HALF = "01";

    // ── 시나리오 A: 연차 결재 차례 도래 ──
    /** A 제목. */
    public static final String TURN_TITLE = "[연차 결재 요청]";
    /** A 본문 템플릿. {@code %s}=신청자명(평문). */
    public static final String TURN_BODY_FORMAT = "%s님이 신청한 연차 결재를 기다리고 있습니다.";

    // ── 시나리오 B: 무결재 연차 사용 통보 ──
    /** B 제목. */
    public static final String USED_TITLE = "[연차 사용 통보]";
    /** B 종일 본문. {@code %s}=신청자명 / 날짜(YYYY-MM-DD) / 일수. */
    public static final String USED_BODY_FULL_FORMAT = "%s님이 %s 연차 %s일을 사용했습니다.";
    /** B 반차 본문. {@code %s}=신청자명 / 날짜(YYYY-MM-DD). */
    public static final String USED_BODY_HALF_FORMAT = "%s님이 %s 반차를 사용했습니다.";
    /** B 시간차 본문. {@code %s}=신청자명 / 날짜(YYYY-MM-DD) / 시작(HH:MM) / 종료(HH:MM). */
    public static final String USED_BODY_HOURLY_FORMAT = "%s님이 %s %s~%s 시간차 연차를 사용했습니다.";
}
