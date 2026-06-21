package com.prafta.common.cmm.push;

/**
 * 교대근무 팀 스케줄 변경 통보 PUSH 공용 상수 (prafta-com-016-D-2).
 *
 * <p>관리자가 교대근무 팀을 신규 저장하거나, 조원을 추가하거나, 적용기간을 연장하여
 * 조원의 근무계획이 실제로 덮어씌워졌을 때, 그 조원 본인에게 통보한다.
 * 한 번의 저장에서 그 사람의 여러 날이 바뀌어도 <b>묶어서 1건</b>만 발송한다(D-Q1).
 * 실제 덮인 날이 1건도 없는 조원(전부 연차/OT 보존)은 통보 대상에서 제외한다(D-Q2).
 *
 * <p>BODY/payload 에 PII(이름·휴대폰 등)는 포함하지 않으며, 팀명({@code SHIFT_TEAM_NM})만 본문에 치환한다.
 * DATA_PAYLOAD 는 라우팅 키만 담는다.
 *
 * <p>NOTI_TYPE 은 SYS045 카탈로그에 신규 등록한다(prafta-com-016-d-shift-noti.sql).
 * app-021 푸시설정 토글 W6(교대 스케줄 변경)에 매핑한다(기본 ON, opt-out).
 */
public final class ShiftSchChangeNotiConst {

    private ShiftSchChangeNotiConst() {
    }

    /** SYS045 알림 유형: 교대근무 팀 스케줄 변경 통보(근로자 대상). */
    public static final String NOTI_TYPE = "SHIFT_SCH_CHANGED";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── 메시지 ──
    public static final String TITLE = "[교대근무 스케줄 변경 안내]";

    /**
     * 본문 템플릿. {@code {teamNm}} 자리에 교대근무 팀명(SHIFT_TEAM_NM)을 치환한다.
     * PII 미포함(팀명은 PII 아님).
     */
    public static final String BODY_TEMPLATE =
            "교대근무 팀 [{teamNm}]에 소속되어 근무 스케줄이 변경되었습니다. 확인해주세요.";
}
