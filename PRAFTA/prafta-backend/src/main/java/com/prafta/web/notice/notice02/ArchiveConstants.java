package com.prafta.web.notice.notice02;

/**
 * 자료실(Archive) 상수 (PRAFTA-053).
 *
 * <p>자료타입 코드그룹값({@code tb_baim_val_m.BAIM_VAL_CD})은 YJ 추후 확정·주입 예정이다.
 * 임의 하드코딩 금지(plan §0, 참고 §7-1). 빈 값(미주입) 이면 자료타입 드롭다운이 빈 목록을
 * 반환하고, 생성 시 ARCHIVE_TYPE_CD 유효성 검증이 모두 실패하여 신규 저장이 막힌다(의도된 게이트).
 */
public final class ArchiveConstants {

    /**
     * 자료타입 코드그룹(tb_baim_val_m.BAIM_VAL_CD). 사용자 확정값 COM008 주입(prafta-app-025 J1-8, 2026-06-14).
     * ⚠️ 본 상수는 웹·앱 자료실 공용이다. 주입으로 웹 자료실 등록도 함께 활성화된다(의도된 것).
     */
    public static final String ARCHIVE_BAIM_VAL_CD = "COM008";

    /** tb_notice.NOTICE_TYPE 자료실 분기값. 전 쿼리/저장에 강제. */
    public static final String NOTICE_TYPE_ARCHIVE = "ARCHIVE";

    /** 자료실 첨부 파일타입(SYS010 005=공지첨부 재사용, 047-2). */
    public static final String FILE_TYPE_ARCHIVE = "005";

    /** TARGET_SCOPE NOT NULL 충족용 고정값(자료실은 대상 개념 없음). */
    public static final String TARGET_SCOPE_ALL = "ALL";

    private ArchiveConstants() {
        // 상수 홀더 — 인스턴스화 방지
    }
}
