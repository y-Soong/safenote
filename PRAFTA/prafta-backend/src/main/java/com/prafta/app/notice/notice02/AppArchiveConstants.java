package com.prafta.app.notice.notice02;

import com.prafta.web.notice.notice02.ArchiveConstants;

/**
 * 앱 자료실(Archive) 상수 (prafta-app-025 J1-8).
 *
 * <p>자료타입 코드그룹값/파일타입 등은 웹 공용 {@link ArchiveConstants}를 그대로 재사용한다.
 * 앱 전용 매퍼/서비스는 식별값을 JWT 에서만 도출(IDOR)하고, 웹 컨트롤러/서비스를 런타임 호출하지 않는다
 * (앱 완전분리 — app-010/012/023 선례). 상수만 공유한다.
 */
public final class AppArchiveConstants {

    /** 자료타입 코드그룹(tb_baim_val_m.BAIM_VAL_CD). 웹 공용 상수 재사용(COM008). */
    public static final String ARCHIVE_BAIM_VAL_CD = ArchiveConstants.ARCHIVE_BAIM_VAL_CD;

    /** tb_notice.NOTICE_TYPE 자료실 분기값. 전 쿼리/저장에 강제. */
    public static final String NOTICE_TYPE_ARCHIVE = ArchiveConstants.NOTICE_TYPE_ARCHIVE;

    /** 자료실 첨부 파일타입(SYS010 005=공지첨부 재사용). */
    public static final String FILE_TYPE_ARCHIVE = ArchiveConstants.FILE_TYPE_ARCHIVE;

    private AppArchiveConstants() {
        // 상수 홀더 — 인스턴스화 방지
    }
}
