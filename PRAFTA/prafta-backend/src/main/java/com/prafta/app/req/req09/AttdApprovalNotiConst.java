package com.prafta.app.req.req09;

/**
 * 근태 요청 결재 PUSH(차례 도래 / 자체근태승인 승인 요망) 공용 상수 (PRAFTA-APP-009).
 *
 * <p>SYS045 알림 유형 / 채널·상태 / 메시지 템플릿을 한 곳에 모은다. 연차
 * {@code LeaveApprovalNotiConst} 미러이며, 메시지 문구는 추후 노무 검토 시 본 상수만 교체하면 된다.
 *
 * <p>본문에 합성하는 신청자명은 평문 {@code USER_NM} 조회값이다(AES-GCM 복호화 불필요).
 * DATA_PAYLOAD 에는 평문 PII 를 넣지 않고 라우팅 키만 직렬화한다.
 */
public final class AttdApprovalNotiConst {

    private AttdApprovalNotiConst() {
    }

    // ── SYS045 알림 유형 (tb_noti_outbox.NOTI_TYPE) ──
    /** 'N' 결재라인: 근태 결재 차례 도래(결재자 대상). */
    public static final String NOTI_TYPE_APPROVAL_TURN = "ATTD_APPROVAL_TURN";
    /** 'Y' 자체근태승인: 승인 요망(신청자 소속 노드 관리자 대상). */
    public static final String NOTI_TYPE_APPROVAL_REQUEST = "ATTD_APPROVAL_REQUEST";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── 차례 도래(시나리오 A) ──
    /** 차례 도래 제목. */
    public static final String TURN_TITLE = "[근태 결재 요청]";
    /** 차례 도래 본문 템플릿. {@code %s}=신청자명(평문). */
    public static final String TURN_BODY_FORMAT = "%s님이 신청한 근태 결재를 기다리고 있습니다.";

    // ── 자체근태승인 승인 요망(시나리오 B) ──
    /** 승인 요망 제목. */
    public static final String REQUEST_TITLE = "[근태 승인 요청]";
    /** 승인 요망 본문 템플릿. {@code %s}=신청자명(평문). */
    public static final String REQUEST_BODY_FORMAT = "%s님이 근태 요청을 등록했습니다. 승인 처리해 주세요.";
}
