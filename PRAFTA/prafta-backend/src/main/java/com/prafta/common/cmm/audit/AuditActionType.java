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

    private AuditActionType() {
        // 상수 카탈로그 — 인스턴스화 방지
    }
}
