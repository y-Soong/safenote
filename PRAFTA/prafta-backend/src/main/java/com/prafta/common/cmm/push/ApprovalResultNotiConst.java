package com.prafta.common.cmm.push;

/**
 * 결재 결과 통보(W2) PUSH 공용 상수 (PRAFTA-APP-021-3a).
 *
 * <p>연차 신청(05/06)·근태 보정(01/02)·초과근무 보정(04)의 승인/반려 결과를 신청자 본인에게
 * 통보하는 PUSH 의 SYS045 알림 유형 / 채널·상태 / 메시지 템플릿을 한 곳에 모은다.
 *
 * <p><b>본문 최소 원칙(§8-R 4)</b>: 승인/반려 여부만 알린다. 반려 사유 전문·PII·날짜는
 * BODY/DATA_PAYLOAD 에 절대 포함하지 않는다(앱에서 상세 조회). DATA_PAYLOAD 는 라우팅 키만.
 */
public final class ApprovalResultNotiConst {

    private ApprovalResultNotiConst() {
    }

    // ── SYS045 알림 유형 (tb_noti_outbox.NOTI_TYPE) ──
    /** 연차 신청 승인 결과(신청자 대상). */
    public static final String NOTI_TYPE_LEAVE_APPROVED = "LEAVE_RESULT_APPROVED";
    /** 연차 신청 반려 결과(신청자 대상). */
    public static final String NOTI_TYPE_LEAVE_REJECTED = "LEAVE_RESULT_REJECTED";
    /** 근태/초과근무 보정 승인 결과(신청자 대상). */
    public static final String NOTI_TYPE_ATTD_APPROVED = "ATTD_RESULT_APPROVED";
    /** 근태/초과근무 보정 반려 결과(신청자 대상). */
    public static final String NOTI_TYPE_ATTD_REJECTED = "ATTD_RESULT_REJECTED";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── 메시지 (본문 최소 — 사유/PII 미포함) ──
    /** 연차 결과 제목. */
    public static final String LEAVE_TITLE = "[연차 신청 결과]";
    /** 근태/초과근무 결과 제목. */
    public static final String ATTD_TITLE = "[근태 요청 결과]";
    /** 승인 본문(공통). */
    public static final String BODY_APPROVED = "요청이 승인되었습니다.";
    /** 반려 본문(공통) — 사유 전문은 미포함, 앱에서 확인. */
    public static final String BODY_REJECTED = "요청이 반려되었습니다. 앱에서 사유를 확인하세요.";
}
