package com.prafta.common.cmm.audit.service;

import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.cmm.audit.command.AuditLogCommand;

/**
 * 감사 로그 적재 공통 서비스 (PRAFTA-037-F5).
 *
 * <p>한 줄 호출({@link #record}) 로 INSERT 1행을 적재한다. 본업(다운로드/권한 변경 등)에
 * 영향을 주지 않도록 구현체는 내부 try/catch 와 {@code REQUIRES_NEW} 트랜잭션으로
 * 격리한다 (정책서 §11.3).
 */
public interface AuditLogService {

    /**
     * 감사 로그 1건 적재. 실패 시 호출 측에 예외를 던지지 않는다(log.error 만).
     *
     * @param command   감사 적재 커맨드 (회사/사용자/액션/리소스)
     * @param context   요청 컨텍스트 (IP / User-Agent)
     */
    void record(AuditLogCommand command, AuditContext context);
}
