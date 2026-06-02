package com.prafta.common.cmm.audit.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code tb_audit_log} INSERT 1건의 운반체 (PRAFTA-037-F5).
 *
 * <p>{@link com.prafta.common.cmm.audit.service.impl.AuditLogServiceImpl} 가
 * {@link com.prafta.common.cmm.audit.command.AuditLogCommand} + {@link com.prafta.common.cmm.audit.AuditContext}
 * 에서 만들어 mapper 로 전달.
 *
 * <p>{@code auditId} 는 서비스에서 채번해 세팅. {@code insertDate} 는 DB DEFAULT 사용.
 */
@Getter
@Setter
public class AuditLogInsertVO {

    /** 감사 로그 ID (PK, varchar(25)) — 서비스에서 채번하여 세팅 */
    private String auditId;

    /** 회사 코드 */
    private String cmpnyCd;

    /** 행위자 사용자 코드 — 비로그인 행위는 NULL */
    private String userCd;

    /** 감사 액션 유형 [SYS046] */
    private String actionType;

    /** 대상 리소스 유형 */
    private String resourceType;

    /** 대상 리소스 식별자 — NULL 허용 */
    private String resourceKey;

    /** 요청 IP — NULL 허용 */
    private String ipAddress;

    /** 요청 User-Agent — NULL 허용 (≤500자) */
    private String userAgent;

    /** 추가 페이로드 JSON 문자열 — NULL 허용 (PII 평문 금지) */
    private String detailJson;

    /** 등록자 (=USER_CD or SYSTEM) */
    private String insertNo;
}
