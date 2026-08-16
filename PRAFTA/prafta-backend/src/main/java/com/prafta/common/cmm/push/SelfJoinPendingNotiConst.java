package com.prafta.common.cmm.push;

/**
 * 셀프가입 승인 대기 통보(M6) PUSH 공용 상수.
 *
 * <p>셀프가입이 접수되어 계정이 {@code ACCOUNT_STATUS='06'}(가입승인대기)로 적재된 직후,
 * 신청자 소속 부서 + 상위 부서의 정/부 관리자에게 통보한다. DATA_PAYLOAD 는 라우팅 키만 담는다
 * (신청자 식별자·PII 미포함).
 */
public final class SelfJoinPendingNotiConst {

    private SelfJoinPendingNotiConst() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /** SYS045 알림 유형: 셀프가입 승인 대기(부서 관리자 대상). 16자 ≤ varchar(30). */
    public static final String NOTI_TYPE = "SELFJOIN_PENDING";

    // ── 발송 채널 / 상태 ──
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String SEND_STATUS_PENDING = "PENDING";

    // ── 메시지 ──
    /**
     * 알림 제목.
     *
     * <p>★본문에 건수를 넣지 않는다. dedupKey 가 일별이라 그날 첫 신청 시점의 문구가 고정되고
     * 이후 신청은 UNIQUE 로 흡수되어 갱신이 불가능하다 — 숫자를 박으면 3명이 신청해도 영원히
     * "1건"으로 남는 거짓 정보가 된다. 정확한 건수는 화면(대기 탭)과 런처 배지가 보여준다.
     */
    public static final String TITLE = "[가입 승인 대기]";

    /** 알림 본문. 건수 미포함(위 TITLE javadoc 참조). */
    public static final String BODY = "승인 대기 중인 가입 신청이 있습니다. 확인해 주세요.";

    /**
     * dedupKey 접두(14자).
     *
     * <p>전체 형식 {@code SELFJOIN_PEND_{targetUserCd}_{yyyyMMdd}_{siteCd}} —
     * 14 + USER_CD(≤20) + 1 + 8 + 1 + SITE_CD(≤50) = <b>최대 94자 ≤ varchar(100)</b>.
     */
    public static final String DEDUP_PREFIX = "SELFJOIN_PEND_";

    /** {@code TB_NOTI_OUTBOX.DEDUP_KEY} 컬럼 길이. 초과 시 축약 키로 폴백한다. */
    public static final int DEDUP_KEY_MAX_LEN = 100;
}
