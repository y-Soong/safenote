package com.prafta.common.cmm.push;

/**
 * 위험성평가 검토요청 통보(M5) PUSH 공용 상수 (PRAFTA-APP-021-3d).
 *
 * <p>위험성평가가 "검토 요청"(ASSESSMENT_STATUS='001')으로 전이될 때 사업장 안전관리자(safe) +
 * 노드 main/sub 관리자에게 통보한다(§8-R 1, 합집합·중복제거·본인 제외). DATA_PAYLOAD 는 라우팅 키만.
 */
public final class RiskAssessNotiConst {

    private RiskAssessNotiConst() {
    }

    /** SYS045 알림 유형: 위험성평가 검토 요청(담당/관리자 대상). */
    public static final String NOTI_TYPE = "RISK_ASSESS_REQUESTED";

    /** 검토 요청 상태값(SYS011 등). 본 상태로 전이될 때만 통보한다. */
    public static final String STATUS_REVIEW_REQUESTED = "001";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── 메시지 ──
    public static final String TITLE = "[위험성평가 검토 요청]";
    public static final String BODY = "검토가 필요한 위험성평가가 등록되었습니다.";
}
