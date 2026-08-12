package com.prafta.common.cmm.audit;

/**
 * 감사 액션 유형 상수 카탈로그 (PRAFTA-037-F5).
 *
 * <p>{@code tb_audit_log.ACTION_TYPE} 컬럼에 저장되는 SYS060 코드값.
 * 본 작업(prafta-036 양식 다운로드)에서는 {@link #DOWNLOAD} 한 코드만 사용한다.
 * 향후 권한 변경(02)/상태 변경(03)/조직 변경(04)/삭제(05)/조회(06) 등은 follow-up.
 *
 * <p>2026-05-29 SYS046 → SYS060 이동 (SYS046~SYS055 는 prafta-033 TBM 코드그룹으로 선점됨).
 * 자세한 사유는 {@code prafta-037-F5-audit-log-fix-sys.sql} 참조.
 */
public final class AuditActionType {

    /** 다운로드 (SYS060='01') */
    public static final String DOWNLOAD = "01";

    /**
     * 상태 변경 (SYS060='03') — 소정-04/09 셀프가입 승인·거부.
     *
     * <p>계정 상태 전이(ACCOUNT_STATUS '06 가입승인대기' → '01 활성화' 또는 '07 가입거부')를 남긴다.
     * ★거부 사유의 <b>유일한 보존처</b>이므로(tb_user 에 사유 컬럼 없음) 이 액션의 적재를
     * 임의로 제거하지 말 것.
     *
     * <p>코드값 '03' 은 본 클래스 javadoc 의 예약(02 권한변경/03 상태변경/04 조직변경/05 삭제/06 조회)을
     * 따른다. SYS060 코드표 시드는 {@code sojeong-3-1-user09-user10-menu-seed.sql} 참조.
     */
    public static final String STATUS_CHANGE = "03";

    /**
     * 설정 변경 (SYS060='07') — SMS2-B3/C1.
     *
     * <p>운영 설정값 변경(SMS 발송 임계값 수정 / 킬스위치 자동 발동·수동 해제)에 사용한다.
     *
     * <p>★코드값 '07' 은 {@code prafta-sms-ppurio-2-rate-limit-policy.sql} 의 SYS060 시드와 한 쌍이다.
     *    시드 적용 시 '07' 이 이미 점유돼 있어 다른 번호로 실행했다면 이 상수도 함께 바꿔야 한다.
     *    (02~06 은 위 javadoc 대로 권한/상태/조직/삭제/조회용으로 예약돼 있다)
     */
    public static final String SETTING_CHANGE = "07";

    private AuditActionType() {
        // 상수 카탈로그 — 인스턴스화 방지
    }
}
