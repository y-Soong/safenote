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

    /** SMS 발송 상한 임계값(TB_SMS_SEND_POLICY) 변경 — SMS2-C1 */
    public static final String SMS_SEND_POLICY = "SMS_SEND_POLICY";

    /** SMS 발송 킬스위치 발동/해제 — SMS2-B3(자동 발동) / SMS2-C1(수동 해제) */
    public static final String SMS_KILL_SWITCH = "SMS_KILL_SWITCH";

    private AuditResourceType() {
        // 상수 카탈로그 — 인스턴스화 방지
    }
}
