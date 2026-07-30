package com.prafta.common.cmm.leave.promotion.service;

/**
 * 연차 사용촉진 PUSH(1차 통지 / 2차 직권지정 통보) 공용 상수 (PRAFTA-COM-008-A-2).
 *
 * <p>SYS045 알림 유형 / 채널·상태 / 메시지 템플릿을 한 곳에 모은다.
 * 문구는 추후 노무 검토 시 본 상수만 교체하면 된다(LeaveApprovalNotiConst 와 동일 철학).
 *
 * <p>본문에 합성하는 근로자명은 평문 {@code USER_NM} 조회값이다(AES-GCM 복호화 불필요).
 * DATA_PAYLOAD 에는 평문 이름을 넣지 않고 라우팅 키만 직렬화한다.
 */
public final class LeavePromotionNotiConst {

    private LeavePromotionNotiConst() {
    }

    // ── SYS045 알림 유형 (tb_noti_outbox.NOTI_TYPE) ──
    /** 1차: 연차 사용촉진 통지(근로자 — 계획서 제출 요청). */
    public static final String NOTI_TYPE_PROMOTION_NOTICE = "LEAVE_PROMOTION_NOTICE";
    /** 2차: 회사 직권 지정 통보(근로자 — 날짜 나열). A-4 에서 사용. */
    public static final String NOTI_TYPE_PROMOTION_DESIGNATED = "LEAVE_PROMOTION_DESIGNATED";
    /**
     * 1차 독촉(계획 제출 재안내) — <b>재발송이며 새 통지가 아니다</b>(확정 D4).
     *
     * <p>본 문자열은 ① 본 상수 ② SYS045 시드({@code SYST_VAL_D_CD}) ③ 1차 현황 조회 매퍼의
     * 독촉 집계 {@code NOTI_TYPE} 리터럴 3곳이 동일해야 한다. 하나만 어긋나면 독촉 집계가
     * 조용히 0 이 된다.
     */
    public static final String NOTI_TYPE_PROMOTION_REMIND = "LEAVE_PROMOTION_REMIND";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── 1차 통지 ──
    /** 1차 제목. */
    public static final String NOTICE_TITLE = "[연차 사용촉진 안내]";
    /** 1차 본문 템플릿. {@code %s}=근로자명(평문) / 잔여일수. */
    public static final String NOTICE_BODY_FORMAT =
            "%s님, 소멸 임박 연차 %s일이 있습니다. 연차 사용 계획을 등록해 주세요.";

    // ── 2차 직권지정 통보(A-4) ──
    /** 2차 제목. */
    public static final String DESIGNATED_TITLE = "[연차 사용촉진 2차 지정 안내]";
    /** 2차 본문 템플릿. {@code %s}=근로자명(평문) / 지정 날짜 나열(예: "8월 12일, 8월 19일"). */
    public static final String DESIGNATED_BODY_FORMAT =
            "%s님, 연차 사용촉진 2차로 %s 이(가) 지정되었습니다.";

    // ── 1차 독촉(계획 제출 재안내, 확정 D4) ──
    /** 1차 독촉 제목. "재안내" 를 명시해 최초 통지로 오해시키지 않는다. */
    public static final String REMIND_TITLE = "[연차 사용 계획 제출 안내(재안내)]";
    /**
     * 1차 독촉 본문 템플릿. {@code %s}=근로자명(평문) / 제출 기한(예: "8월 12일").
     *
     * <p>문구 원칙(D4): 독촉은 <b>재발송</b>이므로 법정 10일 통지 요건을 되살리지 않는다.
     * "앞서 안내드린 … 재안내" 로 최초 통지가 아님을 명시하고, "새로 촉구한다 / 기한이 다시
     * 시작된다" 류 표현을 쓰지 않는다.
     */
    public static final String REMIND_BODY_FORMAT =
            "%s님, 앞서 안내드린 연차 사용촉진에 대한 재안내입니다. 연차 사용 계획 제출 기한은 %s까지입니다. "
          + "기한까지 제출하지 않으면 회사가 남은 연차의 사용 시기를 직접 지정할 수 있습니다.";
}
