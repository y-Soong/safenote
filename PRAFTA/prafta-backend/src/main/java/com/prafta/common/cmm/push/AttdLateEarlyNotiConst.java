package com.prafta.common.cmm.push;

/**
 * 지각/조퇴 감지 통보(M1) PUSH 공용 상수 (PRAFTA-APP-021-3c).
 *
 * <p>raw 실근태(표준화 미적용, §8-R/사용자 결정 D) 기준으로 지각/조퇴를 감지해 근로자 소속 노드의
 * main/sub 관리자에게 통보한다. 지각/조퇴를 통합 1개 NOTI_TYPE 로 두고 본문에서 구분한다.
 * 본문의 근로자명은 평문 USER_NM(복호화 불필요). DATA_PAYLOAD 는 라우팅 키만(평문 미포함).
 */
public final class AttdLateEarlyNotiConst {

    private AttdLateEarlyNotiConst() {
    }

    /** SYS045 알림 유형: 지각/조기퇴근 감지(노드 관리자 대상, 통합). */
    public static final String NOTI_TYPE = "ATTD_LATE_EARLY_DETECTED";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── 메시지 (본문에서 지각/조퇴 구분) ──
    public static final String LATE_TITLE = "[지각 감지]";
    /** {@code %s}=근로자명(평문). */
    public static final String LATE_BODY_FORMAT = "%s님이 지각했습니다.";
    public static final String EARLY_TITLE = "[조기퇴근 감지]";
    /** {@code %s}=근로자명(평문). */
    public static final String EARLY_BODY_FORMAT = "%s님이 조기퇴근했습니다.";

    // ── 이벤트 종류(서비스 내부 분기/payload) ──
    public static final String EVENT_LATE = "LATE";
    public static final String EVENT_EARLY = "EARLY";
}
