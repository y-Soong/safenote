package com.prafta.common.cmm.audit.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 감사 로그 적재 요청 커맨드 (PRAFTA-037-F5).
 *
 * <p>{@link com.prafta.common.cmm.audit.service.AuditLogService#record} 의 입력.
 * 회사/사용자/액션/리소스 식별자와 선택적 페이로드를 담는다. IP/UA 는
 * {@link com.prafta.common.cmm.audit.AuditContext} 로 별도 전달.
 *
 * <p>{@code detailJson} 에는 PII 평문 금지 (정책서 §11.1).
 */
@Getter
@Builder
public class AuditLogCommand {

    /** 회사 코드 — 토큰에서만 결정 (IDOR 방지) */
    private final String cmpnyCd;

    /** 행위자 사용자 코드 — 비로그인 행위는 null */
    private final String userCd;

    /** 감사 액션 유형 — {@link com.prafta.common.cmm.audit.AuditActionType} 상수 사용 */
    private final String actionType;

    /** 대상 리소스 유형 — {@link com.prafta.common.cmm.audit.AuditResourceType} 상수 사용 */
    private final String resourceType;

    /** 대상 리소스 식별자 — 양식 다운로드 등 단일 리소스 대상은 null */
    private final String resourceKey;

    /** 추가 페이로드 JSON 문자열 — PII 평문 금지, 없으면 null */
    private final String detailJson;
}
