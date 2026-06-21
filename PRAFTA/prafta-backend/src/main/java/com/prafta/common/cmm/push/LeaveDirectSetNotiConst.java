package com.prafta.common.cmm.push;

/**
 * 관리자 연차/월차 직접 등록 통보 PUSH 공용 상수 (prafta-com-016-C-2).
 *
 * <p>근무계획관리(Attd_05)에서 관리자가 근로자의 셀에 연차/월차를 직접 등록(직접 차감)했을 때,
 * 그 근로자 본인에게 통보한다. 여러 날을 한 번에 등록해도 <b>묶어서 1건</b>만 발송한다.
 * BODY/payload 에 PII(이름·휴대폰 등)는 포함하지 않으며 DATA_PAYLOAD 는 라우팅 키만 담는다.
 *
 * <p>NOTI_TYPE 은 SYS045 카탈로그에 신규 등록한다(prafta-com-016-c-leave-set-noti.sql).
 * app-021 푸시설정 토글 W1 (연차 관련 근로자 알림) 그룹에 매핑한다(기본 ON, opt-out).
 */
public final class LeaveDirectSetNotiConst {

    private LeaveDirectSetNotiConst() {
    }

    /** SYS045 알림 유형: 관리자 연차/월차 직접 등록 통보(근로자 대상). */
    public static final String NOTI_TYPE = "LEAVE_DIRECT_SET";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── 메시지 ──
    public static final String TITLE = "[연차 등록 안내]";
}
