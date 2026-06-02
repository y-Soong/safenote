package com.prafta.common.cmm.audit;

/**
 * 감사 대상 리소스 유형 상수 카탈로그 (PRAFTA-037-F5).
 *
 * <p>{@code tb_audit_log.RESOURCE_TYPE} 컬럼에 저장되는 자유 식별자.
 * SYS코드가 아닌 고정 enum 카탈로그로 관리 (코드 변경 시 컴파일 타임 검증 가능).
 * 새 리소스 추가 시 본 카탈로그에 상수 1줄 추가.
 */
public final class AuditResourceType {

    /** 관리자 신규 사용자 생성 엑셀 양식 다운로드 */
    public static final String USER_CREATE_TEMPLATE = "USER_CREATE_TEMPLATE";

    private AuditResourceType() {
        // 상수 카탈로그 — 인스턴스화 방지
    }
}
